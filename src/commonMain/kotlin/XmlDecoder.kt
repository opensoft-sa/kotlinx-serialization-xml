package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder

/**
 * Decoder used by [Xml] during deserialization. This interface can be used to inject desired
 * behaviour into a serialization process of [Xml].
 */
public interface XmlDecoder : Decoder {
    /** An instance of the current [Xml]. */
    public val xml: Xml

    /**
     * Decodes the next element in the current input as [XmlElement]. The type of the decoded
     * element depends on the current state of the input and, when received by
     * [serializer][KSerializer] in its [KSerializer.serialize] method, the type of the token
     * directly matches the [kind][SerialDescriptor.kind].
     *
     * This method is allowed to invoke only as the part of the whole deserialization process of the
     * class, calling this method after invoking [beginStructure] or any `decode*` method will lead
     * to unspecified behaviour.
     */
    public fun decodeXmlElement(): XmlElement
}
