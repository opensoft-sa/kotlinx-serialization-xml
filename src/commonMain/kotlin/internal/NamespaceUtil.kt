package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import pt.opensoft.kotlinx.serialization.xml.DeclaresXmlNamespace
import pt.opensoft.kotlinx.serialization.xml.XmlElement
import pt.opensoft.kotlinx.serialization.xml.XmlNamespace

/** URI of the empty (no) namespace. */
internal const val EMPTY_NAMESPACE_URI = ""
/** Prefix of the default namespace. */
internal const val DEFAULT_NAMESPACE_PREFIX = ""
/** Default prefix used when creating a new namespace prefix. */
internal const val AUTO_NAMESPACE_PREFIX = "ns"

/** Initial namespaces in scope. */
internal val INITIAL_NAMESPACES_IN_SCOPE = mapOf(DEFAULT_NAMESPACE_PREFIX to EMPTY_NAMESPACE_URI)

/** Attribute name for a namespace declaration with the provided [prefix]. */
private fun namespaceDeclarationAttributeName(prefix: String) =
    if (prefix.isEmpty()) "xmlns" else "xmlns:${prefix}"

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
    descriptor: SerialDescriptor,
    namespaces: MutableMap<String, String>,
    addAttribute: (XmlElement.Attribute) -> Unit,
) {
    val namespaceDeclarationAnnotations = descriptor.getXmlNamespaceDeclarations()
    for (declaration in namespaceDeclarationAnnotations) {
        // If the namespace is already in scope with the same prefix, then we don't need to
        // redeclare it, otherwise we always declare the namespace, even if just to change the
        // prefix (which may mean declaring a new default namespace)
        if (namespaces[declaration.prefix] != declaration.uri) {
            addAttribute(
                XmlElement.Attribute(
                    namespaceDeclarationAttributeName(declaration.prefix),
                    declaration.uri
                )
            )
            namespaces[declaration.prefix] = declaration.uri
        }
    }
}

/**
 * Declares a new namespace from an [XmlNamespace] annotation (if it isn't already in scope) by
 * generating a new prefix and adding the new declaration to the namespaces in scope, as well as to
 * the element attributes.
 */
private fun declareNamespaceIfNotInScope(
    namespaceAnnotation: XmlNamespace,
    namespaces: MutableMap<String, String>,
    addAttribute: (XmlElement.Attribute) -> Unit
) {
    if (namespaceAnnotation.uri !in namespaces.values) {
        val prefix = newNamespacePrefix(namespaces, namespaceAnnotation.preferredPrefix)
        addAttribute(
            XmlElement.Attribute(namespaceDeclarationAttributeName(prefix), namespaceAnnotation.uri)
        )
        namespaces[prefix] = namespaceAnnotation.uri
    }
}

/** Gets the namespace of an element, assuming that it is already in scope. */
internal fun getElementNamespace(
    descriptor: SerialDescriptor,
    namespaces: Map<String, String>
): String =
    descriptor.getXmlNamespace()?.uri
        ?: (namespaces[DEFAULT_NAMESPACE_PREFIX] ?: error("Empty namespace not in scope"))

/** Obtains the prefix associated with a given element's namespace. */
internal fun getElementNamespacePrefix(namespace: String, namespaces: Map<String, String>): String {
    // Always prefer the default namespace
    if (namespaces[DEFAULT_NAMESPACE_PREFIX] == namespace) {
        return DEFAULT_NAMESPACE_PREFIX
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
    if (namespace == EMPTY_NAMESPACE_URI) {
        return DEFAULT_NAMESPACE_PREFIX
    }
    // Otherwise, use the prefix that was last defined, but isn't the empty one
    return namespaces.entries
        .filter { (prefix) -> prefix.isNotEmpty() }
        .lastOrNull { (_, uri) -> namespace == uri }
        ?.key ?: error("Namespace '$namespace' not in scope with a non-empty prefix")
}

/** Gets the namespace of an element, and declares it within the element if not already in scope. */
internal fun getAndDeclareElementNamespace(
    descriptor: SerialDescriptor,
    namespaces: MutableMap<String, String>,
    addAttribute: (XmlElement.Attribute) -> Unit
): String {
    val namespaceAnnotation = descriptor.annotations.filterIsInstance<XmlNamespace>().firstOrNull()
    return namespaceAnnotation?.uri?.also {
        declareNamespaceIfNotInScope(namespaceAnnotation, namespaces, addAttribute)
    } ?: (namespaces[DEFAULT_NAMESPACE_PREFIX] ?: error("Empty namespace not in scope"))
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
    val childrenNamespaceAnnotations =
        (0 until descriptor.elementsCount).mapNotNull { i ->
            val namespaceDeclarations = descriptor.getElementXmlNamespaceDeclarations(i)
            val childNamespaceAnnotation = descriptor.getElementXmlNamespace(i)
            if (
                childNamespaceAnnotation != null &&
                    namespaceDeclarations.none { it.uri == childNamespaceAnnotation.uri }
            )
                childNamespaceAnnotation
            else null
        }
    for (childNamespaceAnnotation in childrenNamespaceAnnotations) {
        declareNamespaceIfNotInScope(childNamespaceAnnotation, namespaces, addAttribute)
    }
}
