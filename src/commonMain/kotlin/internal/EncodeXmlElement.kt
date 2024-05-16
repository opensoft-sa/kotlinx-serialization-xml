import pt.opensoft.kotlinx.serialization.xml.*
import pt.opensoft.kotlinx.serialization.xml.XmlElement.Text
import pt.opensoft.kotlinx.serialization.xml.internal.*
import pt.opensoft.kotlinx.serialization.xml.internal.GLOBAL_NAMESPACES
import pt.opensoft.kotlinx.serialization.xml.internal.getAttributeNamespacePrefix
import pt.opensoft.kotlinx.serialization.xml.internal.getElementNamespacePrefix

/** Function used to encode an XML element. */
internal fun Composer.encodeXmlElement(
    element: XmlElement,
    namespaces: Map<String, String> = GLOBAL_NAMESPACES
) {
    val namespacesInScope = namespaces.toMutableMap()
    // Read namespace declarations in the element attributes and add them to the namespaces in
    // scope
    for (attribute in element.attributes) {
        if (attribute.name == XMLNS_NAMESPACE_PREFIX && attribute.namespace == NO_NAMESPACE_URI) {
            namespacesInScope[NO_NAMESPACE_PREFIX] = attribute.value
        } else if (attribute.namespace == XMLNS_NAMESPACE_URI) {
            namespacesInScope[attribute.name] = attribute.value
        }
    }

    // Keep track of extra namespace declarations that we auto-generate due to the element or
    // its attributes having namespaces that haven't been declared
    val autoDeclarations = mutableSetOf<XmlElement.Attribute>()
    val addAttribute: (XmlElement.Attribute) -> Unit = { autoDeclarations += it }

    declareNamespaceIfNeeded(element.namespace, "", false, namespacesInScope, addAttribute)
    val prefix = getElementNamespacePrefix(element.namespace, namespacesInScope)
    startElement(prefix, element.name)

    for (attribute in element.attributes) {
        declareNamespaceIfNeeded(attribute.namespace, "", true, namespacesInScope, addAttribute)
        appendAttribute(
            getAttributeNamespacePrefix(attribute.namespace, namespacesInScope),
            attribute.name,
            attribute.value
        )
    }

    for (attribute in autoDeclarations) {
        appendAttribute(
            getAttributeNamespacePrefix(attribute.namespace, namespacesInScope),
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
                is XmlElement -> encodeXmlElement(item, namespacesInScope)
            }
        }
        unIndent().endElement(prefix, element.name)
    }
}
