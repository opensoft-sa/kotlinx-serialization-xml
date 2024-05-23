package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.*

/** Gets the name of an XML element. */
internal fun SerialDescriptor.getXmlName(): String =
    annotations.filterIsInstance<XmlName>().firstOrNull()?.value
        ?: serialName.substringAfterLast(".")

/** Gets the name of an XML element or attribute at the provided [index]. */
internal fun SerialDescriptor.getElementXmlName(index: Int): String =
    getElementAnnotations(index).filterIsInstance<XmlName>().firstOrNull()?.value
        ?: getElementName(index)

/** Gets the name of a wrapped XML element at the provided [index]. */
internal fun SerialDescriptor.getElementXmlWrappedName(index: Int): String? =
    getElementAnnotations(index).filterIsInstance<XmlWrappedName>().firstOrNull()?.value

/** Gets the list of namespace declarations of an XML element. */
internal fun SerialDescriptor.getXmlNamespaceDeclarations(): List<DeclaresXmlNamespace> =
    annotations.filterIsInstance<DeclaresXmlNamespace>()

/** Gets the namespace of an XML element. */
internal fun SerialDescriptor.getXmlNamespace(): XmlNamespace? =
    annotations.filterIsInstance<XmlNamespace>().firstOrNull()

/** Gets the namespace of an XML element or attribute at the provided [index]. */
internal fun SerialDescriptor.getElementXmlNamespace(index: Int): XmlNamespace? =
    getElementAnnotations(index).filterIsInstance<XmlNamespace>().firstOrNull()

/** Gets the wrapped namespace of an XML element at the provided [index]. */
internal fun SerialDescriptor.getElementXmlWrappedNamespace(index: Int): XmlWrappedNamespace? =
    getElementAnnotations(index).filterIsInstance<XmlWrappedNamespace>().firstOrNull()

/** Whether the element at the provided [index] is an XML attribute. */
internal fun SerialDescriptor.isElementXmlAttribute(index: Int): Boolean =
    getElementAnnotations(index).any { it is XmlAttribute }

/** Whether the element at the provided [index] is an XML text. */
internal fun SerialDescriptor.isElementXmlText(index: Int): Boolean =
    getElementAnnotations(index).any { it is XmlText }

/** Whether the element at the provided [index] has an [XmlWrap] annotation. */
internal fun SerialDescriptor.isElementXmlWrap(index: Int): Boolean =
    getElementAnnotations(index).any { it is XmlWrap }

/**
 * Index of the element encoded as an XML element with the provided [name] and [namespace], given
 * the [namespaces] in scope. Returns [UNKNOWN_NAME] if no such element exists.
 */
internal fun SerialDescriptor.getElementXmlElementIndex(
    name: String,
    namespace: String,
    defaultNamespace: String
): Int {
    for (i in 0 until elementsCount) {
        if (
            !isElementXmlAttribute(i) &&
                !isElementXmlText(i) &&
                name == getElementXmlName(i) &&
                namespace == (getElementXmlNamespace(i)?.uri ?: defaultNamespace)
        ) {
            return i
        }
    }
    return CompositeDecoder.UNKNOWN_NAME
}

/**
 * Index of the element encoded as an XML attribute with the provided [name] and [namespace].
 * Returns [UNKNOWN_NAME] if no such element exists.
 */
internal fun SerialDescriptor.getElementXmlAttributeIndex(name: String, namespace: String): Int {
    for (i in 0 until elementsCount) {
        if (
            isElementXmlAttribute(i) &&
                name == getElementXmlName(i) &&
                namespace == (getElementXmlNamespace(i)?.uri ?: NO_NAMESPACE_URI)
        ) {
            return i
        }
    }
    return CompositeDecoder.UNKNOWN_NAME
}

/** Index of the element encoded as text. Returns [UNKNOWN_NAME] if no such element exists. */
internal fun SerialDescriptor.getElementXmlTextIndex(): Int {
    for (i in 0 until elementsCount) {
        if (isElementXmlText(i)) {
            return i
        }
    }
    return CompositeDecoder.UNKNOWN_NAME
}

/** Validates the annotations of an XML element. */
internal fun SerialDescriptor.validateXmlAnnotations() {
    val name = getXmlName()
    if (name.any { it.isWhitespace() }) {
        throw XmlDescriptorException(this, "XML element names must not contain whitespaces")
    }

    val namespaceDeclarations = getXmlNamespaceDeclarations()
    val namespaces = HashMap<String, String>(namespaceDeclarations.size)
    for (declaration in namespaceDeclarations) {
        if (declaration.prefix == XML_NAMESPACE_PREFIX && declaration.uri != XML_NAMESPACE_URI) {
            throw XmlDescriptorException(
                this,
                "The prefix '$XML_NAMESPACE_PREFIX' must not be bound to a namespace name other " +
                    "than '$XML_NAMESPACE_URI' (see https://www.w3.org/TR/xml-names/#ns-decl)"
            )
        }
        if (declaration.uri == XML_NAMESPACE_URI && declaration.prefix != XML_NAMESPACE_PREFIX) {
            throw XmlDescriptorException(
                this,
                "No prefixes other than '$XML_NAMESPACE_PREFIX' may be bound to " +
                    "'$XML_NAMESPACE_URI' (see https://www.w3.org/TR/xml-names/#ns-decl)"
            )
        }
        if (declaration.prefix in namespaces && namespaces[declaration.prefix] != declaration.uri) {
            throw XmlDescriptorException(
                this,
                "There cannot be multiple namespace declarations with the same prefix " +
                    "'${declaration.prefix}'"
            )
        }
        namespaces[declaration.prefix] = declaration.uri
    }
}

/** Validates the annotations of all elements. */
internal fun SerialDescriptor.validateElementXmlAnnotations() {
    var hasText = false
    (0 until elementsCount).forEach { i ->
        val isAttribute = isElementXmlAttribute(i)
        val isText = isElementXmlText(i)
        val isWrap = isElementXmlWrap(i)

        val hasName = getElementAnnotations(i).filterIsInstance<XmlName>().firstOrNull() != null
        val hasNamespace = getElementXmlNamespace(i) != null
        val hasWrappedName = getElementXmlWrappedName(i) != null
        val hasWrappedNamespace = getElementXmlWrappedNamespace(i) != null

        if (isAttribute) {
            if (isText) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have both @XmlAttribute " +
                        "and @XmlText annotations"
                )
            }
            if (isWrap) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have both @XmlAttribute " +
                        "and @XmlWrap annotations"
                )
            }
        }
        if (isText) {
            if (hasName) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have both @XmlText and " +
                        "@XmlName annotations"
                )
            }
            if (hasNamespace) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have both @XmlText " +
                        "and @XmlNamespace annotations"
                )
            }
            if (isWrap) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have both @XmlText and @XmlWrap " +
                        "annotations"
                )
            }
            if (hasText) {
                throw XmlDescriptorException(
                    this,
                    "No more than one property can be annotated with @XmlText"
                )
            }
            hasText = true
        }
        if (!isWrap) {
            if (hasWrappedName) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have @XmlWrappedName while not " +
                        "having the @XmlWrap annotation"
                )
            }
            if (hasWrappedNamespace) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have @XmlWrappedNamespace while not " +
                        "having the @XmlWrap annotation"
                )
            }
        }
    }
}

// XXX: Workaround for https://github.com/Kotlin/kotlinx.serialization/issues/2535
private val mockSerializersList = List(20) { String.serializer() }

/** Returns the "actual" descriptor, given a possibly contextual or inline one. */
internal fun SerialDescriptor.actualDescriptor(module: SerializersModule): SerialDescriptor =
    when {
        // FIXME: use module.getContextualDescriptor once
        //  https://github.com/Kotlin/kotlinx.serialization/issues/2535 is fixed
        kind == SerialKind.CONTEXTUAL ->
            capturedKClass?.let { klass ->
                module
                    .getContextual(klass, mockSerializersList)
                    ?.descriptor
                    ?.actualDescriptor(module)
            } ?: this
        isInline -> getElementDescriptor(0).actualDescriptor(module)
        else -> this
    }
