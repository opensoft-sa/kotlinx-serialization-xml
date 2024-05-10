package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import pt.opensoft.kotlinx.serialization.xml.internal.appendXmlAttributeValue
import pt.opensoft.kotlinx.serialization.xml.internal.appendXmlText

/** Object allowed as content of an XML element. */
public sealed interface XmlContent

/** Representation of an XML element. */
@Serializable(with = XmlElementSerializer::class)
public class XmlElement(
    public val name: String,
    public val namespace: String = NO_NAMESPACE_URI,
    public val attributes: Set<Attribute> = emptySet(),
    public val content: List<XmlContent> = emptyList(),
) : XmlContent {
    init {
        if (name.any { it.isWhitespace() }) {
            throw IllegalArgumentException("XML element names must not contain whitespaces")
        }
    }

    public constructor(
        name: String,
        attributes: Set<Attribute> = emptySet(),
        content: List<XmlContent> = emptyList()
    ) : this(name, NO_NAMESPACE_URI, attributes, content)

    override fun toString(): String = buildString {
        val namespaceNotation = if (namespace.isNotEmpty()) "{$namespace}" else ""

        append('<').append(namespaceNotation).append(name)
        for (attribute in attributes) {
            append(' ').append(attribute)
        }
        if (content.isEmpty()) {
            append("/>")
        } else {
            append('>')
                .append(content.joinToString(""))
                .append("</")
                .append(namespaceNotation)
                .append(name)
                .append('>')
        }
    }

    override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            other !is XmlElement -> false
            name != other.name -> false
            namespace != other.namespace -> false
            attributes != other.attributes -> false
            content != other.content -> false
            else -> true
        }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }

    /** Attribute of an XML element. */
    public class Attribute(
        public val name: String,
        public val namespace: String = NO_NAMESPACE_URI,
        public val value: String
    ) {
        init {
            if (name.any { it.isWhitespace() }) {
                throw IllegalArgumentException("XML attribute names must not contain whitespaces")
            }
        }

        public constructor(name: String, value: String) : this(name, "", value)

        override fun toString(): String = buildString {
            if (namespace.isNotEmpty()) {
                append('{').append(namespace).append('}')
            }
            append(name).append('=').append('"').appendXmlAttributeValue(value).append('"')
        }

        override fun equals(other: Any?): Boolean =
            when {
                this === other -> true
                other !is Attribute -> false
                name != other.name -> false
                namespace != other.namespace -> false
                value != other.value -> false
                else -> true
            }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + namespace.hashCode()
            result = 31 * result + value.hashCode()
            return result
        }
    }

    /** Text within an XML element. */
    public class Text(public val content: String) : XmlContent {
        override fun toString(): String = buildString { appendXmlText(content) }

        override fun equals(other: Any?): Boolean =
            when {
                this === other -> true
                other !is Text -> false
                content != other.content -> false
                else -> true
            }

        override fun hashCode(): Int = content.hashCode()
    }
}
