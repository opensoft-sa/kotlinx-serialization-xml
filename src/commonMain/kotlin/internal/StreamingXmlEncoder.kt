package pt.opensoft.kotlinx.serialization.xml.internal

import encodeXmlElement
import pt.opensoft.kotlinx.serialization.xml.*

internal class StreamingXmlEncoder(
    override val xml: Xml,
    namespaces: Map<String, String> = GLOBAL_NAMESPACES,
    parentEncoder: StreamingXmlEncoder? = null,
    private val composer: Composer = Composer(xml).appendProlog(xml.configuration.prolog)
) : XmlEncoder, AbstractXmlEncoder<StreamingXmlEncoder>(xml, namespaces, parentEncoder) {
    override fun toString() = composer.toString()

    override fun encodeTransformedXmlElement(element: XmlElement) =
        composer.encodeXmlElement(element, namespaces)

    override fun flatStructureContentEncoder() =
        StreamingXmlEncoder(xml, namespaces, this, composer)

    override fun structureContentEncoder(
        name: String,
        namespace: String,
        namespaceDeclarations: List<XmlElement.Attribute>,
        namespaces: Map<String, String>
    ) = StreamingXmlEncoder(xml, namespaces, this, contentComposer(composer))

    override fun beginStructure(
        name: String,
        namespace: String,
        namespaceDeclarations: List<XmlElement.Attribute>,
        namespaces: Map<String, String>
    ) {
        composer.startElement(getElementNamespacePrefix(namespace, namespaces), name)
        for (namespaceDeclaration in namespaceDeclarations) {
            composer.appendAttribute(
                getAttributeNamespacePrefix(namespaceDeclaration.namespace, namespaces),
                namespaceDeclaration.name,
                namespaceDeclaration.value
            )
        }
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

    override fun wrappedElementContentEncoder(name: String, namespace: String) =
        StreamingXmlEncoder(xml, namespaces, this, contentComposer(composer))

    override fun encodeWrappedElement(
        contentEncoder: StreamingXmlEncoder,
        name: String,
        namespace: String
    ) {
        val prefix = getElementNamespacePrefix(namespace, namespaces)
        composer.startElement(prefix, name)
        if (contentEncoder.composer.isEmpty()) {
            composer.selfEndElement()
        } else {
            composer
                .endElementStart()
                .appendComposer(contentEncoder.composer)
                .endElement(prefix, name)
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
