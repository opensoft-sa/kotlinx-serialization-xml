package pt.opensoft.kotlinx.serialization.xml.internal

/** Escapes an attribute value (assuming the attribute value is declared with double quotes). */
internal fun StringBuilder.appendXmlAttributeValue(value: String) = also {
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
internal fun StringBuilder.appendXmlText(content: String) = also {
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
