package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import pt.opensoft.kotlinx.serialization.xml.*

/** [XmlDecoder] for decoding values from a provided [XmlElement]. */
internal class TreeXmlDecoder(
    xml: Xml,
    private val element: XmlElement,
    namespaces: Map<String, String> = GLOBAL_NAMESPACES,
    defaultNamespace: String = NO_NAMESPACE_URI,
    parentDecoder: TreeXmlDecoder? = null
) : AbstractXmlDecoder<TreeXmlDecoder>(xml, namespaces, defaultNamespace, parentDecoder) {
    /** Attributes of the element as an array. */
    private val elementAttributes = element.attributes.toTypedArray()

    /** Index of the [element]'s attribute currently being decoded. */
    private var attributeIndex = -1
    /** Index of the [element]'s content currently being decoded. */
    private var contentIndex = -1
    /** Whether the [element]'s text content has been decoded. */
    private var decodedText = false

    override fun decodeXmlElement(): XmlElement = element

    override fun flatStructureContentDecoder(defaultNamespace: String): TreeXmlDecoder =
        TreeXmlDecoder(xml, element, namespaces, defaultNamespace, this)

    override fun beginStructure(
        name: String,
        namespace: String,
        namespaces: Map<String, String>,
        defaultNamespace: String,
        isWrapper: Boolean
    ): TreeXmlDecoder {
        var element =
            if (contentIndex >= 0) element.content[contentIndex] as XmlElement else element
        println("beginStructure $name $isWrapper")
        val elementNamespaces = element.collectNamespaces(namespaces)
        if (element.name != name || element.namespace != namespace) {
            fail(
                "Expected element with name '$name' and namespace '$namespace', " +
                    "but found element with name '${element.name}' and namespace " +
                    "'${element.namespace}'"
            )
        }
        if (isWrapper) {
            for (attribute in element.attributes) {
                if (
                    (attribute.name != XMLNS_NAMESPACE_PREFIX ||
                        attribute.namespace != NO_NAMESPACE_URI) &&
                        attribute.namespace != XMLNS_NAMESPACE_URI
                ) {
                    fail(
                        "Unknown attribute with name '${attribute.name}' and namespace " +
                            "'${attribute.namespace}'"
                    )
                }
            }
            element =
                element.content.single() as? XmlElement
                    ?: fail(
                        "Expected element with name '$name' and namespace '$namespace' to " +
                            "contain a single XML element as content"
                    )
        }
        return TreeXmlDecoder(xml, element, elementNamespaces, defaultNamespace, this)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        println("decodeElementIndex ${descriptor.getXmlName()}")
        while (attributeIndex + 1 < elementAttributes.size) {
            val attribute = elementAttributes[++attributeIndex]
            if (
                // Ignore namespace declarations
                attribute.namespace != XML_NAMESPACE_URI &&
                    (attribute.namespace != NO_NAMESPACE_URI ||
                        attribute.name != XMLNS_NAMESPACE_PREFIX)
            ) {
                val attributeIndex =
                    descriptor.getElementXmlAttributeIndex(attribute.name, attribute.namespace)
                if (attributeIndex < 0) {
                    fail(
                        "Unknown attribute with name '${attribute.name}' and namespace " +
                            "'${attribute.namespace}'"
                    )
                }
                println("decodeElementIndex attribute ${attribute.name} $attributeIndex")
                return attributeIndex
            }
        }
        while (contentIndex + 1 < element.content.size) {
            val content = element.content[++contentIndex]
            if (content is XmlElement) {
                val elementIndex =
                    descriptor.getElementXmlElementIndex(
                        content.name,
                        content.namespace,
                        defaultNamespace
                    )
                if (elementIndex < 0) {
                    fail(
                        "Unknown element with name '${content.name}' and namespace " +
                            "'${content.namespace}'"
                    )
                }
                println("decodeElementIndex element ${content.name} $elementIndex")
                return elementIndex
            }
        }
        if (!decodedText) {
            decodedText = true
            val textIndex = descriptor.getElementXmlTextIndex()
            if (textIndex >= 0) {
                println("decodeElementIndex text $textIndex")
                return textIndex
            } else if (element.text != "") {
                fail(
                    "Element has text content but corresponding descriptor has no elements with " +
                        "an @XmlText annotation"
                )
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeAttribute(name: String, namespace: String): String =
        elementAttributes[attributeIndex].value

    override fun decodeText(): String = element.text

    override fun decodeValue(name: String, namespace: String): String {
        println("decodeValue $name")
        val element =
            if (contentIndex >= 0) element.content[contentIndex] as XmlElement else element
        if (element.name != name || element.namespace != namespace) {
            fail(
                "Expected element with name '$name' and namespace '$namespace', " +
                    "but found element with name '${element.name}' and namespace " +
                    "'${element.namespace}'"
            )
        }
        return element.text
    }

    override fun fail(message: String): Nothing = throw XmlDecodingException(message)
}
