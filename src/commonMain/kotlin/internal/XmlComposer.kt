package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.Xml

private const val DEFAULT_SB_CAPACITY = 128

/** Creates an [XmlComposer] from an [Xml] instance. */
internal fun XmlComposer(xml: Xml = Xml.Default): XmlComposer {
    val sb = StringBuilder(DEFAULT_SB_CAPACITY)
    val composer =
        if (xml.configuration.prettyPrint)
            PrettyXmlComposer(sb, xml.configuration.prettyPrintIndent)
        else XmlComposer(sb)
    composer.appendProlog(xml.configuration.prolog)
    return composer
}

/**
 * Creates a composer to compose the content of an element independently from its parent element.
 */
internal fun contentXmlComposer(from: XmlComposer): XmlComposer {
    val sb = StringBuilder(DEFAULT_SB_CAPACITY)
    return if (from is PrettyXmlComposer) PrettyXmlComposer(sb, from) else XmlComposer(sb)
}

internal open class XmlComposer(protected val sb: StringBuilder) {
    fun isEmpty() = sb.isEmpty()

    fun appendProlog(prolog: String?): XmlComposer = also {
        prolog?.let { sb.append(prolog).appendLine() }
    }

    open fun indent(): XmlComposer = this

    open fun unIndent(): XmlComposer = this

    open fun startElement(prefix: String, name: String): XmlComposer = also {
        sb.append("<").appendPrefixedName(prefix, name)
    }

    open fun endElementStart(): XmlComposer = also { sb.append('>') }

    open fun endElement(prefix: String, name: String): XmlComposer = also {
        sb.append("</").appendPrefixedName(prefix, name).append('>')
    }

    open fun selfEndElement(): XmlComposer = also { sb.append("/>") }

    open fun appendAttribute(prefix: String, name: String, value: String): XmlComposer = also {
        sb.append(' ')
            .appendPrefixedName(prefix, name)
            .append('=')
            .append('"')
            .appendXmlAttributeValue(value)
            .append('"')
    }

    open fun appendText(value: String): XmlComposer = also { sb.appendXmlText(value) }

    open fun appendComposer(composer: XmlComposer): XmlComposer = also { sb.append(composer.sb) }

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

/** [XmlComposer] for composing pretty printed XML. */
internal class PrettyXmlComposer(
    sb: StringBuilder,
    private val indent: String,
    private var level: Int = 0,
    private var prevContentWasText: Boolean = false,
) : XmlComposer(sb) {
    internal constructor(
        sb: StringBuilder,
        composer: PrettyXmlComposer
    ) : this(sb, composer.indent, composer.level + 1)

    override fun indent() = also { level += 1 }

    override fun unIndent() = also { level -= 1 }

    override fun startElement(prefix: String, name: String): XmlComposer {
        if (level != 0 && !prevContentWasText) {
            appendLine()
        }
        return super.startElement(prefix, name)
    }

    override fun endElement(prefix: String, name: String): XmlComposer {
        if (!prevContentWasText) {
            appendLine()
        } else {
            prevContentWasText = false
        }
        return super.endElement(prefix, name)
    }

    override fun selfEndElement(): XmlComposer {
        sb.append(' ')
        return super.selfEndElement()
    }

    override fun appendText(value: String): XmlComposer {
        prevContentWasText = true
        return super.appendText(value)
    }

    override fun appendComposer(composer: XmlComposer): XmlComposer {
        prevContentWasText = (composer as PrettyXmlComposer).prevContentWasText
        return super.appendComposer(composer)
    }

    private fun appendLine(): XmlComposer = also { sb.appendLine().append(indent.repeat(level)) }
}
