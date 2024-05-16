package pt.opensoft.kotlinx.serialization.xml

/** Default indent for pretty printing. */
internal const val DEFAULT_PRETTY_PRINT_INDENT = "    "

/** Default map key attribute name. */
internal const val DEFAULT_MAP_KEY_ATTRIBUTE_NAME = "key"

/** Default class descriminator attribute name. */
internal const val DEFAULT_CLASS_DESCRIMINATOR_ATTRIBUTE_NAME = "type"

/**
 * Configuration of the current [Xml] instance available through [Xml.configuration] and configured
 * with [XmlBuilder] constructor.
 *
 * Can be used for debug purposes and for custom Xml-specific serializers via [XmlEncoder] and
 * [XmlDecoder].
 *
 * Standalone configuration object is meaningless and cannot be used outside the [Xml], neither new
 * [Xml] instance can be created from it.
 *
 * Detailed description of each property is available in [XmlBuilder] class.
 */
public class XmlConfiguration
internal constructor(
    public val encodeDefaults: Boolean = false,
    public val prolog: String? = null,
    public val prettyPrint: Boolean = false,
    public val prettyPrintIndent: String = DEFAULT_PRETTY_PRINT_INDENT,
    public val booleanEncoding: BooleanEncoding = BooleanEncoding.NUMERIC,
    public val allowStructuredMapKeys: Boolean = false,
    public val mapKeyAttributeName: String = DEFAULT_MAP_KEY_ATTRIBUTE_NAME,
    public val mapKeyAttributeNamespace: String = NO_NAMESPACE_URI,
    public val classDiscriminatorAttributeName: String = DEFAULT_CLASS_DESCRIMINATOR_ATTRIBUTE_NAME,
    public val classDiscriminatorAttributeNamespace: String = NO_NAMESPACE_URI
) {
    override fun toString(): String {
        return "XmlConfiguration(" +
            "encodeDefaults=$encodeDefaults, " +
            "prolog=$prolog, " +
            "prettyPrint=$prettyPrint, " +
            "prettyPrintIndent='$prettyPrintIndent', " +
            "booleanEncoding=$booleanEncoding, " +
            "allowStructuredMapKeys=$allowStructuredMapKeys, " +
            "mapKeyAttributeName=$mapKeyAttributeName, " +
            "mapKeyAttributeNamespace=$mapKeyAttributeNamespace, " +
            "classDiscriminatorAttributeName=$classDiscriminatorAttributeName, " +
            "classDiscriminatorAttributeNamespace=$classDiscriminatorAttributeNamespace)"
    }
}

/**
 * Defines the type of encoding to use for booleans.
 *
 * Both textual (`true`/`false`) and numeric (`1`/`0`) representations are valid `xsd:boolean`
 * representations.
 */
public enum class BooleanEncoding {
    /** Booleans are encoded as `true` or `false`. */
    TEXTUAL,

    /** Booleans are encoded as `1` or `0`. */
    NUMERIC
}
