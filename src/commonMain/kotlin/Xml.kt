package pt.opensoft.kotlinx.serialization.xml

import kotlin.native.concurrent.ThreadLocal
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.internal.*
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
        val composer = XmlComposer(this)
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
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T =
        StreamingXmlDecoder(this, XmlLexer(string)).decodeSerializableValue(deserializer)

    /**
     * Serializes the given [value] into an equivalent [XmlElement] using the given [serializer]
     *
     * @throws [SerializationException] if the given value cannot be serialized to XML.
     */
    public fun <T> encodeToXmlElement(serializer: SerializationStrategy<T>, value: T): XmlElement {
        val encoder = TreeXmlEncoder(this)
        serializer.serialize(encoder, value)
        return encoder.rootElement
    }

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
    ): T = TreeXmlDecoder(this, element).decodeSerializableValue(deserializer)

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
    return XmlImpl(builder.build(), builder.serializersModule)
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

    /**
     * Prolog to use during encoding. None by default.
     *
     * A new line is automatically appended to the prolog when one is provided.
     *
     * This setting does not affect decoding, where all prologs are ignored.
     */
    public var prolog: String? = xml.configuration.prolog

    /** Specifies whether resulting XML should be pretty-printed. `false` by default. */
    public var prettyPrint: Boolean = xml.configuration.prettyPrint

    /** Specifies indent string to use with [prettyPrint] mode. 4 spaces by default. */
    public var prettyPrintIndent: String = xml.configuration.prettyPrintIndent

    /**
     * Defines the type of encoding to use for booleans. Defaults to [BooleanEncoding.NUMERIC] when
     * [prettyPrint] is `false`, and [BooleanEncoding.TEXTUAL] otherwise.
     *
     * Note that both textual (`true`/`false`) and numeric (`1`/`0`) representations are valid
     * `xsd:boolean` representations and, as such, this setting does not affect decoding.
     */
    public var booleanEncoding: BooleanEncoding? = null

    /**
     * Enables structured objects to be serialized as map keys by changing the serialized form of
     * the map from a list of values with key attributes like:
     * ```xml
     * <el key="k1">v1</el>
     * <el key="k2">v2</el>
     * ```
     *
     * to a flat list of elements like:
     * ```xml
     * <el>k1</el>
     * <el>v1</el>
     * <el>k2</el>
     * <el>v2</el>
     * ```
     *
     * `false` by default.
     *
     * **NOTE**: Map serialisation is not yet implemented.
     */
    public var allowStructuredMapKeys: Boolean = xml.configuration.allowStructuredMapKeys

    /**
     * Specifies the default name of the key attribute for map key serialization. `"key"` by
     * default.
     *
     * **NOTE**: Map serialisation is not yet implemented.
     */
    public var mapKeyAttributeName: String = xml.configuration.mapKeyAttributeName

    /**
     * Specifies the default namespace of the key attribute for map key serialization. No namespace
     * by default.
     *
     * **NOTE**: Map serialisation is not yet implemented.
     */
    public var mapKeyAttributeNamespace: String = xml.configuration.mapKeyAttributeNamespace

    /**
     * Specifies the default name of the class descriptor attribute for polymorphic serialization.
     * `"type"` by default.
     *
     * **NOTE**: Polymorphic serialisation is not yet implemented.
     */
    public var classDiscriminatorAttributeName: String =
        xml.configuration.classDiscriminatorAttributeName

    /**
     * Specifies the default namespace of the class descriptor attribute for polymorphic
     * serialization. No namespace by default.
     *
     * **NOTE**: Polymorphic serialisation is not yet implemented.
     */
    public var classDiscriminatorAttributeNamespace: String =
        xml.configuration.classDiscriminatorAttributeNamespace

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Xml]
     * instance.
     *
     * @see SerializersModule
     * @see Contextual
     * @see Polymorphic
     */
    public var serializersModule: SerializersModule = xml.serializersModule

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
            prolog,
            prettyPrint,
            prettyPrintIndent,
            booleanEncoding
                ?: if (prettyPrint) BooleanEncoding.TEXTUAL else BooleanEncoding.NUMERIC,
            allowStructuredMapKeys,
            mapKeyAttributeName,
            mapKeyAttributeNamespace,
            classDiscriminatorAttributeName,
            classDiscriminatorAttributeNamespace,
        )
    }
}

private class XmlImpl(configuration: XmlConfiguration, module: SerializersModule) :
    Xml(configuration, module)
