package pt.opensoft.kotlinx.serialization.xml

/** Default indent for pretty printing. */
internal const val DEFAULT_PRETTY_PRINT_INDENT = "    "

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
    public val prettyPrint: Boolean = false,
    public val prettyPrintIndent: String = DEFAULT_PRETTY_PRINT_INDENT
) {
    override fun toString(): String {
        return "XmlConfiguration(encodeDefaults=$encodeDefaults, prettyPrint=$prettyPrint, " +
            "prettyPrintIndent='$prettyPrintIndent')"
    }
}
