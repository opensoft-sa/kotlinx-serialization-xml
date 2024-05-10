package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.*

internal class StreamingXmlEncoder(
    override val xml: Xml,
    private val composer: Composer,
    /** Namespaces in scope by prefix. */
    private val namespaces: Map<String, String> = GLOBAL_NAMESPACES,
    private val parentEncoder: StreamingXmlEncoder? = null,
    private val elementDescriptor: SerialDescriptor? = null,
    private val elementIndex: Int = -1,
    private val attributePrefix: String? = null,
    private val attributeName: String? = null
) : XmlEncoder {
    override val serializersModule: SerializersModule
        get() = xml.serializersModule

    private val configuration
        get() = xml.configuration

    private fun elementEncoder(
        elementDescriptor: SerialDescriptor,
        elementIndex: Int,
        attributePrefix: String? = null,
        attributeName: String? = null
    ) =
        StreamingXmlEncoder(
            xml,
            composer,
            namespaces,
            parentEncoder,
            elementDescriptor,
            elementIndex,
            attributePrefix,
            attributeName
        )

    //    private fun childEncoder() = StreamingXmlEncoder()

    override fun encodeXmlElement(element: XmlElement) {
        encodeSerializableValue(XmlElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        configuration.encodeDefaults

    override fun beginStructure(descriptor: SerialDescriptor): StreamingXmlEncoder {
        println("${elementDescriptor?.kind} ${descriptor.kind}")
        if (
            elementDescriptor != null &&
                (descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP)
        ) {
            return this
        }

        descriptor.validateXmlAnnotations()
        descriptor.validateElementXmlAnnotations()

        val namespacesInScope = namespaces.toMutableMap()
        val attributesComposer = Composer(composer)
        val addAttribute: (XmlElement.Attribute) -> Unit = {
            val prefix = getAttributeNamespacePrefix(it.namespace, namespacesInScope)
            attributesComposer.appendAttribute(prefix, it.name, it.value)
        }

        // Declare specified namespaces
        declareSpecifiedNamespaces(
            descriptor.getXmlNamespaceDeclarations(),
            namespacesInScope,
            addAttribute
        )

        // Obtain namespace of the element and declare it if not already in scope
        val namespace =
            getAndDeclareElementNamespace(
                elementDescriptor?.getElementXmlNamespace(elementIndex)
                    ?: descriptor.getXmlNamespace(),
                namespacesInScope,
                addAttribute
            )
        val prefix = getElementNamespacePrefix(namespace, namespacesInScope)

        // Declare children namespaces not in scope
        declareChildrenNamespaces(descriptor, namespacesInScope, addAttribute)

        composer
            .startElement(
                prefix,
                elementDescriptor?.getElementXmlSerialName(elementIndex)
                    ?: descriptor.getXmlSerialName()
            )
            .appendComposer(attributesComposer)

        return StreamingXmlEncoder(xml, Composer(composer), namespacesInScope, this)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (
            elementDescriptor != null &&
                (descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP)
        ) {
            return
        }

        if (composer.isEmpty()) {
            parentEncoder!!.composer.selfEndElement()
            return
        }

        val namespace =
            getElementNamespace(
                parentEncoder!!
                    .elementDescriptor
                    ?.getElementXmlNamespace(parentEncoder.elementIndex)
                    ?: descriptor.getXmlNamespace(),
                namespaces
            )
        val prefix = getElementNamespacePrefix(namespace, namespaces)

        parentEncoder.composer
            .endElementStart()
            .appendComposer(composer)
            .endElement(
                prefix,
                parentEncoder.elementDescriptor?.getElementXmlSerialName(parentEncoder.elementIndex)
                    ?: descriptor.getXmlSerialName()
            )
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        encodeElement(descriptor, index) { encoder ->
            encoder.encodeSerializableValue(serializer, value)
        }
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        encodeElement(descriptor, index) { encoder ->
            encoder.encodeNullableSerializableValue(serializer, value)
        }
    }

    private fun encodeElement(
        descriptor: SerialDescriptor,
        index: Int,
        encodeValue: (encoder: StreamingXmlEncoder) -> Unit
    ) {
        // Encoding of list/map items
        if (descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP) {
            encodeValue(this)
            return
        }
        // Element is to be encoded as an attribute
        if (descriptor.isElementXmlAttribute(index)) {
            val namespace = descriptor.getElementXmlNamespace(index)?.uri ?: NO_NAMESPACE_URI
            val attributePrefix = getAttributeNamespacePrefix(namespace, namespaces)
            val attributeName = descriptor.getElementXmlSerialName(index)
            encodeValue(elementEncoder(descriptor, index, attributePrefix, attributeName))
            return
        }
        // Element is being encoded as text
        if (descriptor.isElementXmlText(index)) {
            encodeValue(elementEncoder(descriptor, index))
            return
        }

        if (descriptor.getElementDescriptor(index).kind is StructureKind) {
            if (descriptor.isElementXmlWrap(index)) {
                encodeWrappedElement(descriptor, index, encodeValue)
            } else {
                encodeValue(elementEncoder(descriptor, index))
            }
            return
        }

        encodeWrappedElement(descriptor, index, encodeValue)
    }

    private fun encodeWrappedElement(
        descriptor: SerialDescriptor,
        index: Int,
        encodeValue: (encoder: StreamingXmlEncoder) -> Unit
    ) {
        val namespace = getElementNamespace(descriptor.getElementXmlNamespace(index), namespaces)
        val prefix = getElementNamespacePrefix(namespace, namespaces)
        val name = descriptor.getElementXmlSerialName(index)

        val isStructure = descriptor.getElementDescriptor(index).kind is StructureKind
        val contentEncoder =
            StreamingXmlEncoder(
                xml,
                Composer(composer),
                namespaces,
                this,
                if (isStructure) null else descriptor,
                if (isStructure) -1 else index
            )
        encodeValue(contentEncoder)

        composer.startElement(prefix, name)
        if (contentEncoder.composer.isEmpty()) {
            composer.selfEndElement()
        } else {
            composer
                .endElementStart()
                .appendComposer(contentEncoder.composer)
                .endElement(prefix, name)
        }
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) =
        encodeStringElement(descriptor, index, value.toString())

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) =
        encodeStringElement(descriptor, index, value.toString())

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) =
        encodeStringElement(descriptor, index, value.toString())

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) =
        encodeStringElement(descriptor, index, value.toString())

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) =
        encodeStringElement(descriptor, index, value.toString())

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) =
        encodeStringElement(descriptor, index, value.toString())

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) =
        encodeStringElement(descriptor, index, value.toString())

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) =
        encodeStringElement(descriptor, index, value.toString())

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) =
        encodeSerializableElement(descriptor, index, String.serializer(), value)

    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder = this

    private fun encodeValue(descriptor: SerialDescriptor, value: String) {
        // No element descriptor is defined: this means that this value is not a property of a
        // structure, in which case we need to wrap it in an element
        if (elementDescriptor == null) {
            val encoder = beginStructure(descriptor)
            encoder.composer.appendText(value)
            encoder.endStructure(descriptor)
            return
        }

        // Value is being encoded as the attribute of an element
        if (attributeName != null) {
            parentEncoder!!.composer.appendAttribute(attributePrefix!!, attributeName, value)
            return
        }

        // Value is being encoded as the text of an element
        composer.appendText(value)
    }

    override fun encodeNull() {
        if (parentEncoder == null) {
            throw XmlSerializationException("Root element must not be null.")
        }
    }

    override fun encodeBoolean(value: Boolean) =
        encodeValue(
            Boolean.serializer().descriptor,
            if (configuration.booleanEncoding === BooleanEncoding.TEXTUAL) value.toString()
            else if (value) "1" else "0"
        )

    override fun encodeByte(value: Byte) =
        encodeValue(Byte.serializer().descriptor, value.toString())

    override fun encodeShort(value: Short) =
        encodeValue(Short.serializer().descriptor, value.toString())

    override fun encodeInt(value: Int) = encodeValue(Int.serializer().descriptor, value.toString())

    override fun encodeLong(value: Long) =
        encodeValue(Long.serializer().descriptor, value.toString())

    override fun encodeFloat(value: Float) =
        encodeValue(Float.serializer().descriptor, value.toString())

    override fun encodeDouble(value: Double) =
        encodeValue(Double.serializer().descriptor, value.toString())

    override fun encodeChar(value: Char) =
        encodeValue(Char.serializer().descriptor, value.toString())

    override fun encodeString(value: String) = encodeValue(String.serializer().descriptor, value)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        encodeValue(enumDescriptor, enumDescriptor.getElementName(index))

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this
}
