package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import pt.opensoft.kotlinx.serialization.xml.*

/** Gets the XML name of an element. */
internal fun SerialDescriptor.getXmlName(): String? =
    annotations.filterIsInstance<XmlName>().firstOrNull()?.value

/** Gets the XML name of an element. */
internal fun SerialDescriptor.getElementXmlName(index: Int): String? =
    getElementAnnotations(index).filterIsInstance<XmlName>().firstOrNull()?.value
        ?: getElementDescriptor(index).annotations.filterIsInstance<XmlName>().firstOrNull()?.value

/** Gets the serial name of an XML element. */
internal fun SerialDescriptor.getXmlSerialName(): String =
    getXmlName() ?: serialName.substringAfterLast(".")

/** Gets the serial name of an XML element or attribute at the provided [index]. */
internal fun SerialDescriptor.getElementXmlSerialName(index: Int): String =
    getElementXmlName(index)
        ?: getElementDescriptor(index).let { descriptor ->
            if (this.kind is StructureKind.LIST || this.kind is StructureKind.MAP)
                descriptor.getXmlSerialName()
            else descriptor.getXmlName() ?: getElementName(index)
        }

/** Gets the list of namespace declarations of an XML element. */
internal fun SerialDescriptor.getXmlNamespaceDeclarations(): List<DeclaresXmlNamespace> =
    annotations.filterIsInstance<DeclaresXmlNamespace>()

/** Gets the namespace of an XML element. */
internal fun SerialDescriptor.getXmlNamespace(): XmlNamespace? =
    annotations.filterIsInstance<XmlNamespace>().firstOrNull()

/** Gets the namespace of an XML element or attribute at the provided [index]. */
internal fun SerialDescriptor.getElementXmlNamespace(index: Int): XmlNamespace? =
    getElementAnnotations(index).filterIsInstance<XmlNamespace>().firstOrNull()
        ?: getElementDescriptor(index).getXmlNamespace()

/** Whether the element at the provided [index] is an XML attribute. */
internal fun SerialDescriptor.isElementXmlAttribute(index: Int): Boolean =
    getElementAnnotations(index).any { it is XmlAttribute }

/** Whether the element at the provided [index] is an XML text. */
internal fun SerialDescriptor.isElementXmlText(index: Int): Boolean =
    getElementAnnotations(index).any { it is XmlText }

/** Whether the element at the provided [index] has an [XmlWrap] annotation. */
internal fun SerialDescriptor.isElementXmlWrap(index: Int): Boolean =
    getElementAnnotations(index).any { it is XmlWrap }

/** Validates the annotations of an XML element. */
internal fun SerialDescriptor.validateXmlAnnotations() {
    val namespaceDeclarations = getXmlNamespaceDeclarations()
    val namespaces = HashMap<String, String>(namespaceDeclarations.size)
    for (declaration in namespaceDeclarations) {
        if (declaration.prefix == XML_NAMESPACE_PREFIX && declaration.uri != XML_NAMESPACE_URI) {
            throw XmlDescriptorException(
                this,
                "The prefix '$XML_NAMESPACE_PREFIX' must not be bound to a namespace name other " +
                    "than '$XML_NAMESPACE_URI' (see https://www.w3.org/TR/xml-names/#ns-decl)."
            )
        }
        if (declaration.uri == XML_NAMESPACE_URI && declaration.prefix != XML_NAMESPACE_PREFIX) {
            throw XmlDescriptorException(
                this,
                "No prefixes other than '$XML_NAMESPACE_PREFIX' may be bound to " +
                    "'$XML_NAMESPACE_URI' (see https://www.w3.org/TR/xml-names/#ns-decl)."
            )
        }
        if (declaration.prefix in namespaces && namespaces[declaration.prefix] != declaration.uri) {
            throw XmlDescriptorException(
                this,
                "There cannot be multiple namespace declarations with the same prefix " +
                    "'${declaration.prefix}'."
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
        val descriptor = getElementDescriptor(i)
        val hasNamespace = descriptor.getXmlNamespace() != null
        val hasXmlName = descriptor.getXmlName() != null

        if (isText) {
            if (isAttribute) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have both @XmlAttribute " +
                        "and @XmlText annotations."
                )
            }
            if (hasXmlName) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have both @XmlText and " +
                        "@XmlName annotations."
                )
            }
            if (hasNamespace) {
                throw XmlDescriptorException(
                    this,
                    "Property '${getElementName(i)}' cannot have both @XmlText " +
                        "and @XmlNamespace annotations."
                )
            }
            if (hasText) {
                throw XmlDescriptorException(
                    this,
                    "No more than one property can be annotated with @XmlText."
                )
            }
            hasText = true
        }
    }
}
