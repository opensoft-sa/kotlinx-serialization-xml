package pt.opensoft.kotlinx.serialization.xml

import kotlin.native.concurrent.ThreadLocal
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.internal.*
import pt.opensoft.kotlinx.serialization.xml.internal.Composer
import pt.opensoft.kotlinx.serialization.xml.internal.StreamingXmlDecoder
import pt.opensoft.kotlinx.serialization.xml.internal.StreamingXmlEncoder
import pt.opensoft.kotlinx.serialization.xml.internal.XmlLexer

/**
 * The main entry point to work with XML serialization. It is typically used by constructing an
 * application-specific instance, with configured XML-specific behaviour and, if necessary,
 * registered in [SerializersModule] custom serializers. `Xml` instance can be configured in its
 * `Xml {}` factory function using [XmlBuilder]. For demonstration purposes or trivial usages, Xml
 * [companion][Xml.Default] can be used instead.
 *
 * Then constructed instance can be used either as regular [SerialFormat] or [StringFormat] or for
 * converting objects to [XmlElement] back and forth.
 *
 * This is the only serial format which has the first-class [XmlElement] support. Any serializable
 * class can be serialized to or from [XmlElement] with [Xml.decodeFromXmlElement] and
 * [Xml.encodeToXmlElement] respectively or serialize properties of [XmlElement] type.
 *
 * Xml instance also exposes its [configuration] that can be used in custom serializers that rely on
 * [XmlDecoder] and [XmlEncoder] for customizable behaviour.
 */
public sealed class Xml(
    public val configuration: XmlConfiguration,
    override val serializersModule: SerializersModule,
) : StringFormat {
    /** The default instance of [Xml] with default configuration. */
    @ThreadLocal
    public companion object Default : Xml(XmlConfiguration(), EmptySerializersModule())

    /**
     * Serializes the [value] into an equivalent XML using the given [serializer].
     *
     * @throws [SerializationException] if the given value cannot be serialized to XML.
     */
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val composer = Composer(this)
        val encoder = StreamingXmlEncoder(this, composer)
        serializer.serialize(encoder, value)
        return composer.toString()
    }

    /**
     * Decodes and deserializes the given XML [string] to the value of type [T] using deserializer
     * retrieved from the reified type parameter.
     *
     * @throws SerializationException in case of any decoding-specific error.
     * @throws IllegalArgumentException if the decoded input is not a valid instance of [T].
     */
    public inline fun <reified T> decodeFromString(string: String): T =
        decodeFromString(serializersModule.serializer(), string)

    /**
     * Deserializes the given XML [string] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given XML string is not a valid XML input for the
     *   type [T].
     * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid
     *   instance of type [T].
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val lexer = XmlLexer(string)
        val input = StreamingXmlDecoder(this, lexer)
        return input.decodeSerializableValue(deserializer)
    }

    /**
     * Serializes the given [value] into an equivalent [XmlElement] using the given [serializer]
     *
     * @throws [SerializationException] if the given value cannot be serialized to XML.
     */
    public fun <T> encodeToXmlElement(serializer: SerializationStrategy<T>, value: T): XmlElement =
        writeXml(this, value, serializer)

    /**
     * Deserializes the given [element] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given XML element is not a valid XML input for the
     *   type [T].
     * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid
     *   instance of type [T].
     */
    public fun <T> decodeFromXmlElement(
        deserializer: DeserializationStrategy<T>,
        element: XmlElement
    ): T = readXml(this, element, deserializer)

    /**
     * Deserializes the given XML [string] into a corresponding [XmlElement] representation.
     *
     * @throws [SerializationException] if the given string is not a valid XML.
     */
    public fun parseToXmlElement(string: String): XmlElement =
        decodeFromString(XmlElementSerializer, string)
}

/**
 * Creates an instance of [Xml] configured from the optionally given [Xml instance][from] and
 * adjusted with [builderAction].
 */
public fun Xml(from: Xml = Xml.Default, builderAction: XmlBuilder.() -> Unit): Xml {
    val builder = XmlBuilder(from)
    builder.builderAction()
    return XmlImpl(builder.build(), from.serializersModule)
}

/**
 * Serializes the given [value] into an equivalent [XmlElement] using a serializer retrieved from
 * reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to XML.
 */
public inline fun <reified T> Xml.encodeToXmlElement(value: T): XmlElement =
    encodeToXmlElement(serializersModule.serializer(), value)

/**
 * Deserializes the given [xml] element into a value of type [T] using a deserializer retrieved from
 * reified type parameter.
 *
 * @throws [SerializationException] if the given XML element is not a valid XML input for the type
 *   [T].
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance
 *   of type [T].
 */
public inline fun <reified T> Xml.decodeFromXmlElement(xml: XmlElement): T =
    decodeFromXmlElement(serializersModule.serializer(), xml)

/** Builder of the [Xml] instance provided by `Xml { ... }` factory function. */
public class XmlBuilder internal constructor(xml: Xml) {
    /**
     * Specifies whether default values of Kotlin properties should be encoded. `false` by default.
     */
    public var encodeDefaults: Boolean = xml.configuration.encodeDefaults

    /** Specifies whether resulting XML should be pretty-printed. `false` by default. */
    public var prettyPrint: Boolean = xml.configuration.prettyPrint

    /** Specifies indent string to use with [prettyPrint] mode. 4 spaces by default. */
    public var prettyPrintIndent: String = xml.configuration.prettyPrintIndent

    internal fun build(): XmlConfiguration {
        if (!prettyPrint) {
            require(prettyPrintIndent == DEFAULT_PRETTY_PRINT_INDENT) {
                "Indent should not be specified when default printing mode is used"
            }
        } else if (prettyPrintIndent != DEFAULT_PRETTY_PRINT_INDENT) {
            val allWhitespaces =
                prettyPrintIndent.all { it == ' ' || it == '\t' || it == '\r' || it == '\n' }
            require(allWhitespaces) {
                "Only whitespace, tab, newline and carriage return are allowed as pretty print " +
                    "symbols. Had $prettyPrintIndent"
            }
        }

        return XmlConfiguration(
            encodeDefaults,
            prettyPrint,
            prettyPrintIndent,
        )
    }
}

private class XmlImpl(configuration: XmlConfiguration, module: SerializersModule) :
    Xml(configuration, module)
