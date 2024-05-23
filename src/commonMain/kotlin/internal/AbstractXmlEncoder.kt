package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.*

/**
 * Abstract XML encoder providing the base functionality for both streaming and tree XML encoders.
 */
internal abstract class AbstractXmlEncoder<TEncoder : AbstractXmlEncoder<TEncoder>>(
    override val xml: Xml,
    override val namespaces: Map<String, String>,
    protected val parentEncoder: TEncoder?
) : XmlEncoder {
    final override val serializersModule: SerializersModule
        get() = xml.serializersModule

    private val configuration
        get() = xml.configuration

    private var elementDescriptor: SerialDescriptor? = null
    private var elementIndex: Int = -1
    private var wrappedElementDescriptor: SerialDescriptor? = null
    private var wrappedElementIndex: Int = -1
    private var flattenStructure: Boolean = false

    final override fun shouldEncodeElementDefault(
        descriptor: SerialDescriptor,
        index: Int
    ): Boolean = configuration.encodeDefaults

    /**
     * Appends the given XML [element] to the current output.
     *
     * The XML element has already been transformed as necessary.
     */
    protected abstract fun encodeTransformedXmlElement(element: XmlElement)

    final override fun encodeXmlElement(element: XmlElement) {
        val transformedElement =
            if (wrappedElementDescriptor == null && elementDescriptor == null) element
            else {
                val name =
                    wrappedElementDescriptor?.getElementXmlWrappedName(wrappedElementIndex)
                        ?: elementDescriptor?.getElementXmlName(elementIndex)
                        ?: element.name
                val namespace =
                    getElementNamespace(
                        wrappedElementDescriptor
                            ?.getElementXmlWrappedNamespace(wrappedElementIndex)
                            ?.uri
                            ?: elementDescriptor?.getElementXmlNamespace(elementIndex)?.uri
                            ?: element.namespace,
                        namespaces
                    )
                element.copy(name = name, namespace = namespace)
            }
        encodeTransformedXmlElement(transformedElement)
    }

    /** Returns the encoder used to encode the content of a flat structure. */
    protected abstract fun flatStructureEncoder(): TEncoder

    /**
     * Encodes the beginning of the XML element representing a non-flat structure and returns an
     * encoder used to encode its content.
     *
     * Provided are the [name] and [namespace] of the XML element being encoded, the list of
     * [namespace declarations][namespaceDeclarations] that need to be declared in the XML element,
     * and the [namespaces] currently in scope.
     */
    protected abstract fun beginStructure(
        name: String,
        namespace: String,
        namespaceDeclarations: List<XmlElement.Attribute>,
        namespaces: Map<String, String>,
    ): TEncoder

    final override fun beginStructure(descriptor: SerialDescriptor): TEncoder {
        descriptor.validateXmlAnnotations()
        descriptor.validateElementXmlAnnotations()

        if (flattenStructure) {
            val contentEncoder = flatStructureEncoder()
            contentEncoder.elementDescriptor = elementDescriptor
            contentEncoder.elementIndex = elementIndex
            contentEncoder.wrappedElementDescriptor = wrappedElementDescriptor
            contentEncoder.wrappedElementIndex = wrappedElementIndex
            return contentEncoder
        }

        val namespaces = namespaces.toMutableMap()
        val namespaceDeclarations = mutableListOf<XmlElement.Attribute>()
        val declareNamespace: (XmlElement.Attribute) -> Unit = { namespaceDeclarations += it }

        // Declare specified namespaces
        declareSpecifiedNamespaces(
            descriptor.getXmlNamespaceDeclarations(),
            namespaces,
            declareNamespace
        )

        // Name of the element
        val name =
            wrappedElementDescriptor?.getElementXmlWrappedName(wrappedElementIndex)
                ?: elementDescriptor?.getElementXmlName(elementIndex)
                ?: descriptor.getXmlName()

        // Obtain namespace of the element and declare it if not already in scope
        val namespace =
            getAndDeclareElementNamespace(
                wrappedElementDescriptor?.getElementXmlWrappedNamespace(wrappedElementIndex)?.uri
                    ?: elementDescriptor?.getElementXmlNamespace(elementIndex)?.uri
                    ?: descriptor.getXmlNamespace()?.uri,
                wrappedElementDescriptor
                    ?.getElementXmlWrappedNamespace(wrappedElementIndex)
                    ?.preferredPrefix
                    ?: elementDescriptor?.getElementXmlNamespace(elementIndex)?.preferredPrefix
                    ?: descriptor.getXmlNamespace()?.preferredPrefix,
                namespaces,
                declareNamespace
            )

        // Declare children namespaces not in scope
        declareChildrenNamespaces(descriptor, namespaces, declareNamespace)

        return beginStructure(name, namespace, namespaceDeclarations, namespaces)
    }

    /**
     * Encodes the ending of the XML element representing a non-flat structure.
     *
     * Provided are the [name] and [namespace] of the XML element being encoded.
     */
    protected open fun endStructure(name: String, namespace: String) {}

    final override fun endStructure(descriptor: SerialDescriptor) {
        if (parentEncoder!!.flattenStructure) {
            return
        }

        val name =
            parentEncoder.wrappedElementDescriptor?.getElementXmlWrappedName(
                parentEncoder.wrappedElementIndex
            )
                ?: parentEncoder.elementDescriptor?.getElementXmlName(parentEncoder.elementIndex)
                ?: descriptor.getXmlName()
        val namespace =
            getElementNamespace(
                parentEncoder.wrappedElementDescriptor
                    ?.getElementXmlWrappedNamespace(parentEncoder.wrappedElementIndex)
                    ?.uri
                    ?: parentEncoder.elementDescriptor
                        ?.getElementXmlNamespace(parentEncoder.elementIndex)
                        ?.uri
                    ?: descriptor.getXmlNamespace()?.uri,
                namespaces
            )
        endStructure(name, namespace)
    }

    private fun encodeElement(
        descriptor: SerialDescriptor,
        index: Int,
        encodeValue: (encoder: AbstractXmlEncoder<TEncoder>) -> Unit
    ) {
        // Encoding of a list or map item
        if (descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP) {
            // TODO: Implement support for encoding map keys as attributes
            encodeValue(this)
            return
        }

        // TODO: Support encoding of polymorphic objects via type attribute

        val elementKind =
            descriptor.getElementDescriptor(index).actualDescriptor(serializersModule).kind
        val isCollection = elementKind == StructureKind.LIST || elementKind == StructureKind.MAP

        // Whether element should be wrapped
        if (descriptor.isElementXmlWrap(index)) {
            val name = descriptor.getElementXmlName(index)
            val namespace =
                getElementNamespace(descriptor.getElementXmlNamespace(index)?.uri, namespaces)

            val contentEncoder = beginStructure(name, namespace, emptyList(), namespaces)
            contentEncoder.wrappedElementDescriptor = descriptor
            contentEncoder.wrappedElementIndex = index
            contentEncoder.flattenStructure = isCollection
            encodeValue(contentEncoder)
            contentEncoder.endStructure(name, namespace)
        } else {
            elementDescriptor = descriptor
            elementIndex = index
            flattenStructure = isCollection
            encodeValue(this)
        }
    }

    final override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        encodeElement(descriptor, index) { encoder ->
            encoder.encodeSerializableValue(serializer, value)
        }
    }

    final override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        encodeElement(descriptor, index) { encoder ->
            encoder.encodeNullableSerializableValue(serializer, value)
        }
    }

    final override fun encodeBooleanElement(
        descriptor: SerialDescriptor,
        index: Int,
        value: Boolean
    ) = encodeSerializableElement(descriptor, index, Boolean.serializer(), value)

    final override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) =
        encodeSerializableElement(descriptor, index, Byte.serializer(), value)

    final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) =
        encodeSerializableElement(descriptor, index, Short.serializer(), value)

    final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) =
        encodeSerializableElement(descriptor, index, Int.serializer(), value)

    final override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) =
        encodeSerializableElement(descriptor, index, Long.serializer(), value)

    final override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) =
        encodeSerializableElement(descriptor, index, Float.serializer(), value)

    final override fun encodeDoubleElement(
        descriptor: SerialDescriptor,
        index: Int,
        value: Double
    ) = encodeSerializableElement(descriptor, index, Double.serializer(), value)

    final override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) =
        encodeSerializableElement(descriptor, index, Char.serializer(), value)

    final override fun encodeStringElement(
        descriptor: SerialDescriptor,
        index: Int,
        value: String
    ) = encodeSerializableElement(descriptor, index, String.serializer(), value)

    final override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder = this

    /**
     * Encodes a provided string [value] as an attribute with the provided [name] and [namespace] of
     * an XML element.
     *
     * [parentEncoder] is guaranteed to exist when encoding a value as an attribute.
     */
    protected abstract fun encodeAttribute(name: String, namespace: String, value: String)

    /**
     * Encodes a provided string [value] as text of an XML element.
     *
     * [parentEncoder] is guaranteed to exist when encoding a value as text.
     */
    protected abstract fun encodeText(value: String)

    /**
     * Encodes an XML element with the provided [name] and [namespace] whose content is the provided
     * string [value].
     */
    protected abstract fun encodeValue(name: String, namespace: String, value: String)

    private fun encodeValue(descriptor: SerialDescriptor, value: String) {
        // Value is being encoded as the attribute of an XML element
        if (elementDescriptor?.isElementXmlAttribute(elementIndex) == true) {
            val name = elementDescriptor!!.getElementXmlName(elementIndex)
            val namespace =
                elementDescriptor!!.getElementXmlNamespace(elementIndex)?.uri ?: NO_NAMESPACE_URI
            encodeAttribute(name, namespace, value)
            return
        }

        // Value is being encoded as the text of an XML element
        if (elementDescriptor?.isElementXmlText(elementIndex) == true) {
            encodeText(value)
            return
        }

        // Value is being encoded as an XML element
        val name =
            wrappedElementDescriptor?.getElementXmlWrappedName(wrappedElementIndex)
                ?: elementDescriptor?.getElementXmlName(elementIndex)
                ?: descriptor.getXmlName()
        val namespace =
            getElementNamespace(
                wrappedElementDescriptor?.getElementXmlWrappedNamespace(wrappedElementIndex)?.uri
                    ?: elementDescriptor?.getElementXmlNamespace(elementIndex)?.uri
                    ?: descriptor.getXmlNamespace()?.uri,
                namespaces
            )
        encodeValue(name, namespace, value)
    }

    final override fun encodeNull() {
        if (parentEncoder == null) {
            throw XmlException("Root element must not be null.")
        }
    }

    final override fun encodeBoolean(value: Boolean) =
        encodeValue(
            Boolean.serializer().descriptor,
            if (configuration.booleanEncoding === BooleanEncoding.TEXTUAL) value.toString()
            else if (value) "1" else "0"
        )

    final override fun encodeByte(value: Byte) =
        encodeValue(Byte.serializer().descriptor, value.toString())

    final override fun encodeShort(value: Short) =
        encodeValue(Short.serializer().descriptor, value.toString())

    final override fun encodeInt(value: Int) =
        encodeValue(Int.serializer().descriptor, value.toString())

    final override fun encodeLong(value: Long) =
        encodeValue(Long.serializer().descriptor, value.toString())

    final override fun encodeFloat(value: Float) =
        encodeValue(
            Float.serializer().descriptor,
            when (value) {
                Float.NEGATIVE_INFINITY -> "-INF"
                Float.POSITIVE_INFINITY -> "INF"
                else -> value.toString()
            }
        )

    final override fun encodeDouble(value: Double) =
        encodeValue(
            Double.serializer().descriptor,
            when (value) {
                Double.NEGATIVE_INFINITY -> "-INF"
                Double.POSITIVE_INFINITY -> "INF"
                else -> value.toString()
            }
        )

    final override fun encodeChar(value: Char) =
        encodeValue(Char.serializer().descriptor, value.toString())

    final override fun encodeString(value: String) =
        encodeValue(String.serializer().descriptor, value)

    final override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        encodeValue(enumDescriptor, enumDescriptor.getElementXmlName(index))

    final override fun encodeInline(descriptor: SerialDescriptor): Encoder = this
}
