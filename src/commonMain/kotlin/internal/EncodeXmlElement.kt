import pt.opensoft.kotlinx.serialization.xml.*
import pt.opensoft.kotlinx.serialization.xml.XmlElement.Text
import pt.opensoft.kotlinx.serialization.xml.internal.*
import pt.opensoft.kotlinx.serialization.xml.internal.GLOBAL_NAMESPACES
import pt.opensoft.kotlinx.serialization.xml.internal.getAttributeNamespacePrefix
import pt.opensoft.kotlinx.serialization.xml.internal.getElementNamespacePrefix

/** Function used to encode an XML element. */
internal fun XmlComposer.encodeXmlElement(
    element: XmlElement,
    namespaces: Map<String, String> = GLOBAL_NAMESPACES
) {
    val elementNamespaces = element.collectNamespaces(namespaces)

    // Keep track of extra namespace declarations that we auto-generate due to the element or
    // its attributes having namespaces that haven't been declared
    val autoDeclarations = mutableSetOf<XmlElement.Attribute>()
    val addAttribute: (XmlElement.Attribute) -> Unit = { autoDeclarations += it }

    declareNamespaceIfNeeded(element.namespace, "", false, elementNamespaces, addAttribute)
    val prefix = getElementNamespacePrefix(element.namespace, elementNamespaces)
    startElement(prefix, element.name)

    for (attribute in element.attributes) {
        declareNamespaceIfNeeded(attribute.namespace, "", true, elementNamespaces, addAttribute)
        appendAttribute(
            getAttributeNamespacePrefix(attribute.namespace, elementNamespaces),
            attribute.name,
            attribute.value
        )
    }

    for (attribute in autoDeclarations) {
        appendAttribute(
            getAttributeNamespacePrefix(attribute.namespace, elementNamespaces),
            attribute.name,
            attribute.value
        )
    }

    if (element.content.isEmpty()) {
        selfEndElement()
    } else {
        endElementStart().indent()
        for (item in element.content) {
            when (item) {
                is Text -> appendText(item.content)
                is XmlElement -> encodeXmlElement(item, elementNamespaces)
            }
        }
        unIndent().endElement(prefix, element.name)
    }
}
