package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.*

/**
 * Abstract XML decoder providing the base functionality for both streaming and tree XML decoders.
 */
internal abstract class AbstractXmlDecoder<TDecoder : AbstractXmlDecoder<TDecoder>>(
    override val xml: Xml,
    override val namespaces: Map<String, String>,
    protected val defaultNamespace: String,
    protected val parentDecoder: TDecoder?
) : XmlDecoder {
    final override val serializersModule: SerializersModule
        get() = xml.serializersModule

    private val configuration
        get() = xml.configuration

    private var elementDescriptor: SerialDescriptor? = null
    private var elementIndex: Int = -1
    private var wrappedElementDescriptor: SerialDescriptor? = null
    private var wrappedElementIndex: Int = -1
    private var flattenStructure: Boolean = false

    protected abstract fun decodeTransformedXmlElement(): XmlElement

    final override fun decodeXmlElement(): XmlElement {
        return decodeTransformedXmlElement()
    }

    /** Returns the decoder used to decode the content of a flat structure. */
    protected abstract fun flatStructureContentDecoder(defaultNamespace: String): TDecoder

    /**
     * Decodes the beginning of an XML element representing a non-flat structure and returns a
     * decoder to decode its content.
     *
     * Provided are the [name] and [namespace] of the XML element being decoded, as well as the
     * current [namespaces] in scope and the current [defaultNamespace].
     */
    protected abstract fun beginStructure(
        name: String,
        namespace: String,
        namespaces: Map<String, String>,
        defaultNamespace: String
    ): TDecoder

    final override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        descriptor.validateXmlAnnotations()
        descriptor.validateElementXmlAnnotations()

        val defaultNamespace =
            descriptor
                .getXmlNamespaceDeclarations()
                .firstOrNull { it.prefix == NO_NAMESPACE_PREFIX }
                ?.uri ?: defaultNamespace

        if (flattenStructure) {
            val contentDecoder = flatStructureContentDecoder(defaultNamespace)
            contentDecoder.elementDescriptor = elementDescriptor
            contentDecoder.elementIndex = elementIndex
            contentDecoder.wrappedElementDescriptor = wrappedElementDescriptor
            contentDecoder.wrappedElementIndex = wrappedElementIndex
            return contentDecoder
        }

        val name =
            wrappedElementDescriptor?.getElementXmlWrappedName(wrappedElementIndex)
                ?: elementDescriptor?.getElementXmlName(elementIndex)
                ?: descriptor.getXmlName()
        val namespace =
            wrappedElementDescriptor?.getElementXmlWrappedNamespace(wrappedElementIndex)?.uri
                ?: elementDescriptor?.getElementXmlNamespace(elementIndex)?.uri
                ?: descriptor.getXmlNamespace()?.uri
                ?: defaultNamespace

        return beginStructure(name, namespace, namespaces, defaultNamespace)
    }

    /**
     * Decodes the ending of the XML element representing a non-flat structure.
     *
     * Provided are the [name] and [namespace] of the XML element being decoded.
     */
    protected open fun endStructure(name: String, namespace: String) {}

    final override fun endStructure(descriptor: SerialDescriptor) {
        if (parentDecoder!!.flattenStructure) {
            return
        }

        val name =
            parentDecoder.wrappedElementDescriptor?.getElementXmlWrappedName(
                parentDecoder.wrappedElementIndex
            )
                ?: parentDecoder.elementDescriptor?.getElementXmlName(parentDecoder.elementIndex)
                ?: descriptor.getXmlName()
        val namespace =
            parentDecoder.wrappedElementDescriptor
                ?.getElementXmlWrappedNamespace(parentDecoder.wrappedElementIndex)
                ?.uri
                ?: parentDecoder.elementDescriptor
                    ?.getElementXmlNamespace(parentDecoder.elementIndex)
                    ?.uri
                ?: descriptor.getXmlNamespace()?.uri
                ?: defaultNamespace
        endStructure(name, namespace)
    }

//    protected abstract fun decodeXmlElementIndex(descriptor: SerialDescriptor): Int

//    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
//        val index = decodeXmlElementIndex(descriptor)
//        val kind = descriptor.getElementDescriptor(index).kind
//        if (kind == StructureKind.LIST || kind == StructureKind.MAP) {}
//    }

    private fun <T> decodeElement(
        descriptor: SerialDescriptor,
        index: Int,
        previousValue: T?,
        decodeValue: (encoder: AbstractXmlDecoder<TDecoder>) -> T
    ): T {
        // Decoding of a list or map item
        if (descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP) {
            // TODO: Implement support for decoding map keys as attributes
            return decodeValue(this)
        }

        // TODO: Support decoding of polymorphic objects via type attribute

        val elementKind =
            descriptor.getElementDescriptor(index).actualDescriptor(serializersModule).kind
        val isCollection = elementKind == StructureKind.LIST || elementKind == StructureKind.MAP

        // Whether element should be wrapped
        if (descriptor.isElementXmlWrap(index)) {
            val name = descriptor.getElementXmlName(index)
            val namespace = descriptor.getElementXmlNamespace(index)?.uri ?: defaultNamespace

            val contentDecoder = beginStructure(name, namespace, namespaces, defaultNamespace)
            contentDecoder.wrappedElementDescriptor = descriptor
            contentDecoder.wrappedElementIndex = index
            contentDecoder.flattenStructure = isCollection
            val value = decodeValue(contentDecoder)
            contentDecoder.endStructure(name, namespace)
            return value
        } else {
            elementDescriptor = descriptor
            elementIndex = index
            flattenStructure = isCollection
            return decodeValue(this)
        }
    }

    final override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T =
        decodeElement(descriptor, index, previousValue) { decoder ->
            decoder.decodeSerializableValue(deserializer)
        }

    final override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? =
        decodeElement(descriptor, index, previousValue) { decoder ->
            decoder.decodeNullableSerializableValue(deserializer)
        }

    final override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
        decodeSerializableElement(descriptor, index, Boolean.serializer())

    final override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
        decodeSerializableElement(descriptor, index, Byte.serializer())

    final override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
        decodeSerializableElement(descriptor, index, Short.serializer())

    final override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
        decodeSerializableElement(descriptor, index, Int.serializer())

    final override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
        decodeSerializableElement(descriptor, index, Long.serializer())

    final override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
        decodeSerializableElement(descriptor, index, Float.serializer())

    final override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
        decodeSerializableElement(descriptor, index, Double.serializer())

    final override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
        decodeSerializableElement(descriptor, index, Char.serializer())

    final override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
        decodeSerializableElement(descriptor, index, String.serializer())

    final override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder = this

    /**
     * Decodes the attribute with the provided [name] and [namespace] of an XML element.
     *
     * [parentDecoder] is guaranteed to exist when decoding an attribute.
     */
    protected abstract fun decodeAttribute(name: String, namespace: String): String

    /**
     * Decodes the text of an XML element.
     *
     * [parentDecoder] is guaranteed to exist when decoding text.
     */
    protected abstract fun decodeText(): String

    /** Decodes an XML element with the provided [name] and [namespace]. */
    protected abstract fun decodeValue(name: String, namespace: String): String

    private fun decodeValue(descriptor: SerialDescriptor): String {
        // Value is being decoded from the attribute of an XML element
        if (elementDescriptor?.isElementXmlAttribute(elementIndex) == true) {
            val name = elementDescriptor!!.getElementXmlName(elementIndex)
            val namespace =
                elementDescriptor!!.getElementXmlNamespace(elementIndex)?.uri ?: NO_NAMESPACE_URI
            return decodeAttribute(name, namespace)
        }

        // Value is being decoded from the text of an XML element
        if (elementDescriptor?.isElementXmlText(elementIndex) == true) {
            return decodeText()
        }

        // Value is being decoded from an XML element
        val name =
            wrappedElementDescriptor?.getElementXmlWrappedName(wrappedElementIndex)
                ?: elementDescriptor?.getElementXmlName(elementIndex)
                ?: descriptor.getXmlName()
        val namespace =
            wrappedElementDescriptor?.getElementXmlWrappedNamespace(wrappedElementIndex)?.uri
                ?: elementDescriptor?.getElementXmlNamespace(elementIndex)?.uri
                ?: descriptor.getXmlNamespace()?.uri
                ?: defaultNamespace
        return decodeValue(name, namespace)
    }

    final override fun decodeNotNullMark(): Boolean = true

    final override fun decodeNull(): Nothing? {
        TODO("Not yet implemented")
    }

    final override fun decodeBoolean(): Boolean =
        when (val value = decodeValue(Boolean.serializer().descriptor)) {
            "false",
            "0" -> false
            "true",
            "1" -> true
            else -> fail("Expected false, true, 0, or 1, but got '$value'")
        }

    final override fun decodeByte(): Byte {
        val value = decodeValue(Byte.serializer().descriptor)
        return value.toByteOrNull() ?: fail("Failed to parse byte for input '$value'")
    }

    final override fun decodeShort(): Short {
        val value = decodeValue(Short.serializer().descriptor)
        return value.toShortOrNull() ?: fail("Failed to parse short for input '$value'")
    }

    final override fun decodeInt(): Int {
        val value = decodeValue(Int.serializer().descriptor)
        return value.toIntOrNull() ?: fail("Failed to parse int for input '$value'")
    }

    final override fun decodeLong(): Long {
        val value = decodeValue(Long.serializer().descriptor)
        return value.toLongOrNull() ?: fail("Failed to parse long for input '$value'")
    }

    final override fun decodeFloat(): Float =
        when (val value = decodeValue(Float.serializer().descriptor)) {
            "INF",
            "+INF" -> Float.POSITIVE_INFINITY
            "-INF" -> Float.NEGATIVE_INFINITY
            else -> {
                val float = value.toFloatOrNull()
                if (float == null || float.isInfinite()) {
                    fail("Failed to parse float for input '$value'")
                }
                float
            }
        }

    final override fun decodeDouble(): Double =
        when (val value = decodeValue(Double.serializer().descriptor)) {
            "INF",
            "+INF" -> Double.POSITIVE_INFINITY
            "-INF" -> Double.NEGATIVE_INFINITY
            else -> {
                val double = value.toDoubleOrNull()
                if (double == null || double.isInfinite()) {
                    fail("Failed to parse double for input '$value'")
                }
                double
            }
        }

    final override fun decodeChar(): Char {
        val value = decodeValue(Char.serializer().descriptor)
        if (value.length != 1) {
            fail("Expected single char, but got '$value'")
        }
        return value[0]
    }

    final override fun decodeString(): String = decodeValue(String.serializer().descriptor)

    final override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val value = decodeValue(enumDescriptor)
        for (i in 0 until enumDescriptor.elementsCount) {
            if (value == enumDescriptor.getElementXmlName(i)) {
                return i
            }
        }
        fail("${enumDescriptor.getXmlName()} does not contain element with name '$value'")
    }

    final override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

    protected abstract fun fail(message: String): Nothing
}
