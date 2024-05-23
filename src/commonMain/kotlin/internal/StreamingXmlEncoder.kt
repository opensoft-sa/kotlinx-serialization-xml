package pt.opensoft.kotlinx.serialization.xml.internal

import encodeXmlElement
import pt.opensoft.kotlinx.serialization.xml.*

/** [XmlEncoder] for encoding values to an [XmlComposer]. */
internal class StreamingXmlEncoder(
    xml: Xml,
    private val composer: XmlComposer,
    namespaces: Map<String, String> = GLOBAL_NAMESPACES,
    parentEncoder: StreamingXmlEncoder? = null,
) : XmlEncoder, AbstractXmlEncoder<StreamingXmlEncoder>(xml, namespaces, parentEncoder) {
    override fun encodeTransformedXmlElement(element: XmlElement) =
        composer.encodeXmlElement(element, namespaces)

    override fun flatStructureEncoder() = StreamingXmlEncoder(xml, composer, namespaces, this)

    override fun beginStructure(
        name: String,
        namespace: String,
        namespaceDeclarations: List<XmlElement.Attribute>,
        namespaces: Map<String, String>
    ): StreamingXmlEncoder {
        composer.startElement(getElementNamespacePrefix(namespace, namespaces), name)
        for (namespaceDeclaration in namespaceDeclarations) {
            composer.appendAttribute(
                getAttributeNamespacePrefix(namespaceDeclaration.namespace, namespaces),
                namespaceDeclaration.name,
                namespaceDeclaration.value
            )
        }
        return StreamingXmlEncoder(xml, contentXmlComposer(composer), namespaces, this)
    }

    override fun endStructure(name: String, namespace: String) {
        if (composer.isEmpty()) {
            parentEncoder!!.composer.selfEndElement()
        } else {
            parentEncoder!!
                .composer
                .endElementStart()
                .appendComposer(composer)
                .endElement(getElementNamespacePrefix(namespace, namespaces), name)
        }
    }

    override fun encodeAttribute(name: String, namespace: String, value: String) {
        parentEncoder!!
            .composer
            .appendAttribute(getAttributeNamespacePrefix(namespace, namespaces), name, value)
    }

    override fun encodeText(value: String) {
        composer.appendText(value)
    }

    override fun encodeValue(name: String, namespace: String, value: String) {
        val prefix = getElementNamespacePrefix(namespace, namespaces)
        composer.startElement(prefix, name)
        if (value.isEmpty()) {
            composer.selfEndElement()
        } else {
            composer.endElementStart().appendText(value).endElement(prefix, name)
        }
    }
}
