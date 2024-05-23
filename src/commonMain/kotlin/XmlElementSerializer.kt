package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer object providing [SerializationStrategy] and [DeserializationStrategy] for
 * [XmlElement].
 *
 * It can only be used with the [Xml] format.
 */
public object XmlElementSerializer : KSerializer<XmlElement> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("pt.opensoft.kotlinx.serialization.xml.XmlElement")

    override fun serialize(encoder: Encoder, value: XmlElement): Unit =
        encoder.asXmlEncoder().encodeXmlElement(value)

    override fun deserialize(decoder: Decoder): XmlElement =
        decoder.asXmlDecoder().decodeXmlElement()
}

internal fun Encoder.asXmlEncoder() =
    this as? XmlEncoder
        ?: throw IllegalStateException(
            "This serializer can be used only with Xml format. " +
                "Expected Encoder to be XmlEncoder, got ${this::class}"
        )

internal fun Decoder.asXmlDecoder() =
    this as? XmlDecoder
        ?: throw IllegalStateException(
            "This serializer can be used only with Xml format. " +
                "Expected Decoder to be XmlDecoder, got ${this::class}"
        )
