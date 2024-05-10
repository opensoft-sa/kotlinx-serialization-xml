package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import pt.opensoft.kotlinx.serialization.xml.*

/** Default prefix used when creating a new namespace prefix. */
internal const val AUTO_NAMESPACE_PREFIX = "ns"

/** Initial namespaces in scope. */
internal val GLOBAL_NAMESPACES: Map<String, String> =
    mapOf(
        NO_NAMESPACE_PREFIX to NO_NAMESPACE_URI,
        XML_NAMESPACE_PREFIX to XML_NAMESPACE_URI,
        XMLNS_NAMESPACE_PREFIX to XMLNS_NAMESPACE_URI
    )

/** Attribute name for a namespace declaration with the provided [prefixName]. */
private fun namespaceDeclarationAttribute(prefixName: String, uri: String): XmlElement.Attribute =
    if (prefixName.isEmpty()) XmlElement.Attribute(XMLNS_NAMESPACE_PREFIX, uri)
    else XmlElement.Attribute(prefixName, XMLNS_NAMESPACE_URI, uri)

/** Generates a new non-empty namespace prefix different from the ones in scope. */
private fun newNamespacePrefix(namespaces: Map<String, String>, preferredPrefix: String): String {
    val basePrefix = preferredPrefix.ifBlank { AUTO_NAMESPACE_PREFIX }
    var n: Int? = null
    while (namespaces[basePrefix + (n ?: "")] != null) {
        n = (n ?: 0) + 1
    }
    return basePrefix + (n ?: "")
}

/** Declares the namespace declarations specified via [DeclaresXmlNamespace]. */
internal fun declareSpecifiedNamespaces(
    namespaceDeclarationAnnotations: List<DeclaresXmlNamespace>,
    namespaces: MutableMap<String, String>,
    addAttribute: (XmlElement.Attribute) -> Unit,
) {
    for (declaration in namespaceDeclarationAnnotations) {
        // If the namespace is already in scope with the same prefix, then we don't need to
        // redeclare it, otherwise we always declare the namespace, even if just to change the
        // prefix (which may mean declaring a new default namespace)
        if (namespaces[declaration.prefix] != declaration.uri) {
            namespaces[declaration.prefix] = declaration.uri
            addAttribute(namespaceDeclarationAttribute(declaration.prefix, declaration.uri))
        }
    }
}

/**
 * Declares a new namespace from an [XmlNamespace] annotation (if it isn't already in scope) by
 * generating a new prefix and adding the new declaration to the namespaces in scope, as well as to
 * the element attributes.
 */
private fun declareNamespaceIfNeeded(
    namespaceAnnotation: XmlNamespace,
    isAttribute: Boolean,
    namespaces: MutableMap<String, String>,
    addAttribute: (XmlElement.Attribute) -> Unit
) {
    val prefixes = namespaces.filterValues { it == namespaceAnnotation.uri }.keys
    if (
        prefixes.isEmpty() ||
            // When declaring the namespace of an attribute, we must redeclare it with a prefix if
            // it was only available under the default prefix, since unprefixed attributes belong to
            // no namespace rather than to the default one
            (isAttribute && prefixes.size == 1 && NO_NAMESPACE_PREFIX in prefixes)
    ) {
        val prefix = newNamespacePrefix(namespaces, namespaceAnnotation.preferredPrefix)
        namespaces[prefix] = namespaceAnnotation.uri
        addAttribute(namespaceDeclarationAttribute(prefix, namespaceAnnotation.uri))
    }
}

/** Gets the namespace of an element, assuming that it is already in scope. */
internal fun getElementNamespace(
    namespaceAnnotation: XmlNamespace?,
    namespaces: Map<String, String>
): String =
    namespaceAnnotation?.uri
        ?: (namespaces[NO_NAMESPACE_PREFIX] ?: error("Empty namespace not in scope"))

/** Obtains the prefix associated with a given element's namespace. */
internal fun getElementNamespacePrefix(namespace: String, namespaces: Map<String, String>): String {
    // Always prefer the default namespace
    if (namespaces[NO_NAMESPACE_PREFIX] == namespace) {
        return NO_NAMESPACE_PREFIX
    }
    // Otherwise, use the prefix that was last defined
    return namespaces.entries.lastOrNull { (_, v) -> namespace == v }?.key
        ?: error("Namespace '$namespace' not in scope")
}

/** Obtains the prefix associated with a given attribute's namespace. */
internal fun getAttributeNamespacePrefix(
    namespace: String,
    namespaces: Map<String, String>
): String {
    // Attributes without a prefix always use the default namespace
    if (namespace == NO_NAMESPACE_URI) {
        return NO_NAMESPACE_PREFIX
    }
    // Otherwise, use the prefix that was last defined, but isn't the empty one
    return namespaces.entries
        .filter { (prefix) -> prefix.isNotEmpty() }
        .lastOrNull { (_, uri) -> namespace == uri }
        ?.key ?: error("Namespace '$namespace' not in scope with a non-empty prefix")
}

/** Gets the namespace of an element, and declares it within the element if not already in scope. */
internal fun getAndDeclareElementNamespace(
    namespaceAnnotation: XmlNamespace?,
    namespaces: MutableMap<String, String>,
    addAttribute: (XmlElement.Attribute) -> Unit
): String {
    return namespaceAnnotation?.uri?.also {
        declareNamespaceIfNeeded(namespaceAnnotation, false, namespaces, addAttribute)
    } ?: (namespaces[NO_NAMESPACE_PREFIX] ?: error("Empty namespace not in scope"))
}

/**
 * Declares children namespaces not in scope, ignoring those where the child declares its own
 * namespace via [DeclaresXmlNamespace].
 */
internal fun declareChildrenNamespaces(
    descriptor: SerialDescriptor,
    namespaces: MutableMap<String, String>,
    addAttribute: (XmlElement.Attribute) -> Unit
) {
    for (i in 0 until descriptor.elementsCount) {
        val childNamespaceAnnotation = descriptor.getElementXmlNamespace(i)
        if (childNamespaceAnnotation != null) {
            declareNamespaceIfNeeded(
                childNamespaceAnnotation,
                descriptor.isElementXmlAttribute(i),
                namespaces,
                addAttribute
            )
        }
    }
}
