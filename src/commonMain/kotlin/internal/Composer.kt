package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.Xml

private const val DEFAULT_SB_CAPACITY = 128

/** Creates a composer from an [Xml] instance. */
internal fun Composer(xml: Xml = Xml.Default): Composer {
    val sb = StringBuilder(DEFAULT_SB_CAPACITY)
    return if (xml.configuration.prettyPrint)
        ComposerWithPrettyPrint(sb, xml.configuration.prettyPrintIndent)
    else Composer(sb)
}

/**
 * Creates a composer to compose the content of an element independently from its parent element.
 */
internal fun contentComposer(from: Composer): Composer {
    val sb = StringBuilder(DEFAULT_SB_CAPACITY)
    return if (from is ComposerWithPrettyPrint) ComposerWithPrettyPrint(sb, from) else Composer(sb)
}

internal open class Composer(protected val sb: StringBuilder) {
    fun isEmpty() = sb.isEmpty()

    fun appendProlog(prolog: String?): Composer = also {
        prolog?.let { sb.append(prolog).appendLine() }
    }

    open fun indent(): Composer = this

    open fun unIndent(): Composer = this

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

    /** Escapes an attribute value (assuming the attribute value is declared with double quotes). */
    private fun StringBuilder.appendXmlAttributeValue(value: String) = also {
        ensureCapacity(length + value.length)
        var i = 0
        while (i < value.length) {
            when (val c = value[i]) {
                '<' -> append("&lt;")
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                ']' ->
                    if (value[i + 1] == ']' && value[i + 2] == '>') {
                        append("]]&gt;")
                        i += 2
                    } else {
                        append(c)
                    }
                else -> append(c)
            }
            ++i
        }
    }

    /** Escapes a value to be used as text content. */
    private fun StringBuilder.appendXmlText(content: String) = also {
        ensureCapacity(length + content.length)
        var i = 0
        while (i < content.length) {
            when (val c = content[i]) {
                '<' -> append("&lt;")
                '&' -> append("&amp;")
                ']' ->
                    if (content[i + 1] == ']' && content[i + 2] == '>') {
                        append("]]&gt;")
                        i += 2
                    } else {
                        append(c)
                    }
                else -> append(c)
            }
            ++i
        }
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

    override fun indent() = also { level += 1 }

    override fun unIndent() = also { level -= 1 }

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
