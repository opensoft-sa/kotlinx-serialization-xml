package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.*

/**
 * Collects the namespaces declares in this [XmlElement], merging them with the [namespaces] already
 * in scope.
 */
internal fun XmlElement.collectNamespaces(
    namespaces: Map<String, String>
): MutableMap<String, String> {
    val elementNamespaces = namespaces.toMutableMap()
    for (attribute in attributes) {
        if (attribute.name == XMLNS_NAMESPACE_PREFIX && attribute.namespace == NO_NAMESPACE_URI) {
            elementNamespaces[NO_NAMESPACE_PREFIX] = attribute.value
        } else if (attribute.namespace == XMLNS_NAMESPACE_URI) {
            elementNamespaces[attribute.name] = attribute.value
        }
    }
    return elementNamespaces
}
