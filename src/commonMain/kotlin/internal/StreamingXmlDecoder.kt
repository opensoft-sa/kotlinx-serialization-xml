package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import pt.opensoft.kotlinx.serialization.xml.NO_NAMESPACE_URI
import pt.opensoft.kotlinx.serialization.xml.Xml
import pt.opensoft.kotlinx.serialization.xml.XmlDecoder
import pt.opensoft.kotlinx.serialization.xml.XmlElement

/** [XmlDecoder] for decoding values from an [XmlLexer]. */
internal class StreamingXmlDecoder(
    xml: Xml,
    private val lexer: XmlLexer,
    namespaces: Map<String, String> = GLOBAL_NAMESPACES,
    defaultNamespace: String = NO_NAMESPACE_URI,
    parentDecoder: StreamingXmlDecoder? = null
) : AbstractXmlDecoder<StreamingXmlDecoder>(xml, namespaces, defaultNamespace, parentDecoder) {
    override fun decodeXmlElement(): XmlElement = lexer.decodeXmlElement(namespaces)

    override fun flatStructureContentDecoder(defaultNamespace: String): StreamingXmlDecoder =
        StreamingXmlDecoder(xml, lexer, namespaces, defaultNamespace, this)

    override fun beginStructure(
        name: String,
        namespace: String,
        namespaces: Map<String, String>,
        defaultNamespace: String,
        isWrapper: Boolean
    ): StreamingXmlDecoder {
        TODO("Not yet implemented")
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        TODO("Not yet implemented")
    }

    override fun decodeAttribute(name: String, namespace: String): String {
        TODO("Not yet implemented")
    }

    override fun decodeText(): String {
        TODO("Not yet implemented")
    }

    override fun decodeValue(name: String, namespace: String): String {
        TODO("Not yet implemented")
    }

    override fun fail(message: String): Nothing {
        TODO("Not yet implemented")
    }
}
