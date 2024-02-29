package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import pt.opensoft.kotlinx.serialization.xml.DeclaresXmlNamespace
import pt.opensoft.kotlinx.serialization.xml.XmlName
import pt.opensoft.kotlinx.serialization.xml.XmlNamespace

/** Gets the name of an XML element. */
internal fun SerialDescriptor.getXmlName(): String =
    annotations.filterIsInstance<XmlName>().firstOrNull()?.value
        ?: serialName.substringAfterLast(".")

/** Gets the name of an XML element or attribute at the provided [index]. */
internal fun SerialDescriptor.getElementXmlName(index: Int): String =
    getElementAnnotations(index).filterIsInstance<XmlName>().firstOrNull()?.value
        ?: getElementName(index)

/** Gets the list of namespace declarations of an XML element. */
internal fun SerialDescriptor.getXmlNamespaceDeclarations(): List<DeclaresXmlNamespace> =
    annotations.filterIsInstance<DeclaresXmlNamespace>()

/** Gets the list of namespace declarations of an XML element at the provided [index]. */
internal fun SerialDescriptor.getElementXmlNamespaceDeclarations(
    index: Int
): List<DeclaresXmlNamespace> =
    getElementAnnotations(index).filterIsInstance<DeclaresXmlNamespace>() +
        getElementDescriptor(index).annotations.filterIsInstance<DeclaresXmlNamespace>()

/** Gets the namespace of an XML element. */
internal fun SerialDescriptor.getXmlNamespace(): XmlNamespace? =
    annotations.filterIsInstance<XmlNamespace>().firstOrNull()

/** Gets the namespace of an XML element or attribute at the provided [index]. */
internal fun SerialDescriptor.getElementXmlNamespace(index: Int): XmlNamespace? =
    getElementAnnotations(index).filterIsInstance<XmlNamespace>().firstOrNull()
        ?: getElementDescriptor(index).annotations.filterIsInstance<XmlNamespace>().firstOrNull()
