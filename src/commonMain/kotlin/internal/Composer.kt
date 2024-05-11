package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.Xml

private const val DEFAULT_SB_CAPACITY = 128

/** Creates a composer from an [Xml] instance. */
internal fun Composer(xml: Xml): Composer {
    val sb = StringBuilder(DEFAULT_SB_CAPACITY)
    return if (xml.configuration.prettyPrint)
        ComposerWithPrettyPrint(sb, xml.configuration.prettyPrintIndent)
    else Composer(sb)
}

/** Creates a composer from another composer. */
internal fun Composer(from: Composer): Composer {
    val sb = StringBuilder(DEFAULT_SB_CAPACITY)
    return if (from is ComposerWithPrettyPrint) ComposerWithPrettyPrint(sb, from) else Composer(sb)
}

internal open class Composer(protected val sb: StringBuilder) {
    fun isEmpty() = sb.isEmpty()

    open fun startElement(prefix: String, name: String): Composer = also {
        sb.append("<").appendPrefixedName(prefix, name)
    }

    open fun endElementStart(): Composer = also { sb.append('>') }

    open fun endElement(prefix: String, name: String): Composer = also {
        sb.append("</").appendPrefixedName(prefix, name).append('>')
    }

    open fun selfEndElement(): Composer = also { sb.append("/>") }

    open fun appendAttribute(prefix: String, name: String, value: String): Composer = also {
        sb.append(' ')
            .appendPrefixedName(prefix, name)
            .append('=')
            .append('"')
            .appendXmlAttributeValue(value)
            .append('"')
    }

    open fun appendText(value: String): Composer = also { sb.appendXmlText(value) }

    open fun appendComposer(composer: Composer): Composer = also { sb.append(composer.sb) }

    override fun toString() = sb.toString()

    private fun StringBuilder.appendPrefixedName(prefix: String, name: String): StringBuilder =
        also {
            if (prefix.isNotEmpty()) {
                append(prefix).append(':')
            }
            append(name)
        }
}

internal class ComposerWithPrettyPrint(
    sb: StringBuilder,
    private val indent: String,
    private var level: Int = 0,
    private var prevContentWasText: Boolean = false,
) : Composer(sb) {
    internal constructor(
        sb: StringBuilder,
        composer: ComposerWithPrettyPrint
    ) : this(sb, composer.indent, composer.level + 1)

    override fun startElement(prefix: String, name: String): Composer {
        if (level != 0 && !prevContentWasText) {
            appendLine()
        }
        return super.startElement(prefix, name)
    }

    override fun endElement(prefix: String, name: String): Composer {
        if (!prevContentWasText) {
            appendLine()
        } else {
            prevContentWasText = false
        }
        return super.endElement(prefix, name)
    }

    override fun selfEndElement(): Composer {
        sb.append(' ')
        return super.selfEndElement()
    }

    override fun appendText(value: String): Composer {
        prevContentWasText = true
        return super.appendText(value)
    }

    override fun appendComposer(composer: Composer): Composer {
        prevContentWasText = (composer as ComposerWithPrettyPrint).prevContentWasText
        return super.appendComposer(composer)
    }

    private fun appendLine(): Composer = also { sb.appendLine().append(indent.repeat(level)) }
}
