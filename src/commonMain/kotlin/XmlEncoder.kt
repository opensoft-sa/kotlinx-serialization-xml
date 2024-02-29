package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder

/**
 * Encoder used by [Xml] during serialization. This interface can be used to inject desired
 * behaviour into a serialization process of [Xml].
 */
public interface XmlEncoder : Encoder, CompositeEncoder {
    /** An instance of the current [Xml]. */
    public val xml: Xml

    /**
     * Appends the given XML [element] to the current output. This method is allowed to invoke only
     * as the part of the whole serialization process of the class, calling this method after
     * invoking [beginStructure] or any `encode*` method will lead to unspecified behaviour and may
     * produce an invalid XML result.
     */
    public fun encodeXmlElement(element: XmlElement)
}
