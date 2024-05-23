package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.*

/** [XmlEncoder] for encoding values to an [XmlElement], accessible via [rootElement]. */
internal class TreeXmlEncoder(
    xml: Xml,
    namespaces: Map<String, String> = GLOBAL_NAMESPACES,
    parentEncoder: TreeXmlEncoder? = null,
    private val parentElement: XmlElement? = null
) : XmlEncoder, AbstractXmlEncoder<TreeXmlEncoder>(xml, namespaces, parentEncoder) {
    lateinit var rootElement: XmlElement

    private fun addElementToParent(element: XmlElement) {
        if (parentElement != null) {
            parentElement.content as MutableList += element
        } else {
            rootElement = element
        }
    }

    override fun encodeTransformedXmlElement(element: XmlElement) = addElementToParent(element)

    override fun flatStructureEncoder(): TreeXmlEncoder =
        TreeXmlEncoder(xml, namespaces, this, parentElement)

    override fun beginStructure(
        name: String,
        namespace: String,
        namespaceDeclarations: List<XmlElement.Attribute>,
        namespaces: Map<String, String>
    ): TreeXmlEncoder {
        val element =
            XmlElement(name, namespace, namespaceDeclarations.toMutableSet(), mutableListOf())
        addElementToParent(element)
        return TreeXmlEncoder(xml, namespaces, this, element)
    }

    override fun encodeAttribute(name: String, namespace: String, value: String) {
        parentElement!!.attributes as MutableSet += XmlElement.Attribute(name, namespace, value)
    }

    override fun encodeText(value: String) {
        parentElement!!.content as MutableList += XmlElement.Text(value)
    }

    override fun encodeValue(name: String, namespace: String, value: String) {
        addElementToParent(
            XmlElement(
                name,
                namespace,
                if (value.isEmpty()) emptyList() else listOf(XmlElement.Text(value))
            )
        )
    }
}
