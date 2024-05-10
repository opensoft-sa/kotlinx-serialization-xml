package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** XML element serializer. */
public object XmlElementSerializer : KSerializer<XmlElement> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("pt.opensoft.kotlinx.serialization.xml.XmlElement")

    override fun serialize(encoder: Encoder, value: XmlElement) {
        val xmlEncoder = encoder.asXmlEncoder()
        xmlEncoder.encodeXmlElement(value)
    }

    override fun deserialize(decoder: Decoder): XmlElement {
        val result = decoder.asXmlDecoder().decodeXmlElement()
        return result
    }
}

private fun verify(decoder: Decoder) {
    decoder.asXmlDecoder()
}

private fun verify(encoder: Encoder) {
    encoder.asXmlEncoder()
}

internal fun Decoder.asXmlDecoder() =
    this as? XmlDecoder
        ?: throw IllegalStateException(
            "This serializer can be used only with Xml format. " +
                "Expected Decoder to be XmlDecoder, got ${this::class}"
        )

internal fun Encoder.asXmlEncoder() =
    this as? XmlEncoder
        ?: throw IllegalStateException(
            "This serializer can be used only with Xml format. " +
                "Expected Encoder to be XmlEncoder, got ${this::class}"
        )
