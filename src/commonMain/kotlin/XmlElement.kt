package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import pt.opensoft.kotlinx.serialization.xml.internal.Composer

/** Object allowed as content of an XML element. */
public sealed class XmlContent {
    internal abstract fun composeClarkNotation(composer: Composer)
}

/**
 * Class representing single XML element.
 *
 * [XmlElement.composeClarkNotation] prints an XML tree in clark-notation.
 *
 * The whole hierarchy is [serializable][Serializable] only by [Xml] format.
 *
 * @property name Name of the element.
 * @property namespace Namespace of the element.
 * @property attributes Attributes of the element.
 * @property content Content of the element, normalized without empty text.
 */
@Serializable(with = XmlElementSerializer::class)
public class XmlElement(
    public val name: String,
    public val namespace: String,
    public val attributes: Set<Attribute> = emptySet(),
    content: List<XmlContent> = emptyList()
) : XmlContent() {
    public val content: List<XmlContent> = content.filterNot { it is Text && it.content.isEmpty() }

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

    public constructor(
        name: String,
        namespace: String,
        content: List<XmlContent>
    ) : this(name, namespace, emptySet(), content)

    public constructor(
        name: String,
        content: List<XmlContent>
    ) : this(name, NO_NAMESPACE_URI, emptySet(), content)

    /** Creates a new XML element, having this XML element as base. */
    public fun copy(
        name: String = this.name,
        namespace: String = this.namespace,
        attributes: Set<Attribute> = this.attributes,
        content: List<XmlContent> = this.content
    ): XmlElement = XmlElement(name, namespace, attributes, content)

    /** Prints the XML element in clark-notation (pretty printed by default). */
    override fun toString(): String = toString(true)

    /** Prints the XML element in clark-notation, possibly prettily. */
    public fun toString(
        prettyPrint: Boolean,
        prettyPrintIndent: String = DEFAULT_PRETTY_PRINT_INDENT
    ): String {
        val composer =
            Composer(
                Xml {
                    this.prettyPrint = prettyPrint
                    this.prettyPrintIndent = prettyPrintIndent
                }
            )
        composeClarkNotation(composer)
        return composer.toString()
    }

    override fun composeClarkNotation(composer: Composer) {
        val namespaceNotation = if (namespace.isNotEmpty()) "{$namespace}" else ""
        composer.startElement(NO_NAMESPACE_PREFIX, namespaceNotation + name)
        for (attribute in attributes) {
            attribute.composeClarkNotation(composer)
        }
        if (content.isEmpty()) {
            composer.selfEndElement()
        } else {
            composer.endElementStart().indent()
            for (item in content) {
                item.composeClarkNotation(composer)
            }
            composer.unIndent().endElement(NO_NAMESPACE_PREFIX, namespaceNotation + name)
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

        public constructor(name: String, value: String) : this(name, NO_NAMESPACE_URI, value)

        /** Creates a new XML attribute, having this XML attribute as base. */
        public fun copy(
            name: String = this.name,
            namespace: String = this.namespace,
            value: String = this.value
        ): Attribute = Attribute(name, namespace, value)

        /** Prints the XML attribute in clark-notation. */
        override fun toString(): String {
            val composer = Composer()
            composeClarkNotation(composer)
            return composer.toString().trimStart()
        }

        internal fun composeClarkNotation(composer: Composer) {
            val namespaceNotation = if (namespace.isNotEmpty()) "{$namespace}" else ""
            composer.appendAttribute(NO_NAMESPACE_PREFIX, namespaceNotation + name, value)
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
    public class Text(public val content: String) : XmlContent() {
        /** Prints the XML text content, escaped if necessary. */
        override fun toString(): String {
            val composer = Composer()
            composeClarkNotation(composer)
            return composer.toString()
        }

        override fun composeClarkNotation(composer: Composer) {
            composer.appendText(content)
        }

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
