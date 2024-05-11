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
    private val attributeName: String? = null,
    private val flattenStructure: Boolean = false,
) : XmlEncoder {
    override val serializersModule: SerializersModule
        get() = xml.serializersModule

    private val configuration
        get() = xml.configuration

    private fun elementEncoder(
        elementDescriptor: SerialDescriptor,
        elementIndex: Int,
        attributePrefix: String? = null,
        attributeName: String? = null,
        flattenStructure: Boolean = false
    ) =
        StreamingXmlEncoder(
            xml,
            composer,
            namespaces,
            parentEncoder,
            elementDescriptor,
            elementIndex,
            attributePrefix,
            attributeName,
            flattenStructure
        )

    private fun contentEncoder(
        namespaces: Map<String, String> = this.namespaces,
        flattenStructure: Boolean = false
    ) =
        StreamingXmlEncoder(
            xml,
            Composer(composer),
            namespaces,
            this,
            flattenStructure = flattenStructure
        )

    override fun encodeXmlElement(element: XmlElement) {
        encodeSerializableValue(XmlElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        configuration.encodeDefaults

    override fun beginStructure(descriptor: SerialDescriptor): StreamingXmlEncoder {
        if (flattenStructure) {
            return StreamingXmlEncoder(
                xml,
                composer,
                namespaces,
                this,
                elementDescriptor,
                elementIndex
            )
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

        return contentEncoder(namespacesInScope)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (parentEncoder!!.flattenStructure) {
            return
        }

        if (composer.isEmpty()) {
            parentEncoder.composer.selfEndElement()
            return
        }

        val namespace =
            getElementNamespace(
                parentEncoder.elementDescriptor?.getElementXmlNamespace(parentEncoder.elementIndex)
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

    private fun encodeElement(
        descriptor: SerialDescriptor,
        index: Int,
        encodeValue: (encoder: StreamingXmlEncoder) -> Unit
    ) {
        // Encoding a list or map item
        if (descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP) {
            encodeValue(this)
            return
        }

        val elementKind = descriptor.getElementDescriptor(index).kind
        val isCollection = elementKind == StructureKind.LIST || elementKind == StructureKind.MAP

        // Wrap element
        if (descriptor.isElementXmlWrap(index)) {
            val namespace =
                getElementNamespace(descriptor.getElementXmlNamespace(index), namespaces)
            val prefix = getElementNamespacePrefix(namespace, namespaces)
            val name = descriptor.getElementXmlSerialName(index)

            val contentEncoder = contentEncoder(flattenStructure = isCollection)
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
            return
        } else {
            encodeValue(elementEncoder(descriptor, index, flattenStructure = isCollection))
        }
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

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) =
        encodeSerializableElement(descriptor, index, Boolean.serializer(), value)

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) =
        encodeSerializableElement(descriptor, index, Byte.serializer(), value)

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) =
        encodeSerializableElement(descriptor, index, Short.serializer(), value)

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) =
        encodeSerializableElement(descriptor, index, Int.serializer(), value)

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) =
        encodeSerializableElement(descriptor, index, Long.serializer(), value)

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) =
        encodeSerializableElement(descriptor, index, Float.serializer(), value)

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) =
        encodeSerializableElement(descriptor, index, Double.serializer(), value)

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) =
        encodeSerializableElement(descriptor, index, Char.serializer(), value)

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) =
        encodeSerializableElement(descriptor, index, String.serializer(), value)

    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder = this

    private fun encodeValue(descriptor: SerialDescriptor, value: String) {
        if (elementDescriptor != null) {
            // Value is being encoded as the attribute of an XML element
            if (elementDescriptor.isElementXmlAttribute(elementIndex)) {
                val namespace =
                    elementDescriptor.getElementXmlNamespace(elementIndex)?.uri ?: NO_NAMESPACE_URI
                val attributePrefix = getAttributeNamespacePrefix(namespace, namespaces)
                val attributeName = elementDescriptor.getElementXmlSerialName(elementIndex)
                parentEncoder!!.composer.appendAttribute(attributePrefix, attributeName, value)
                return
            }

            // Value is being encoded as the text of an XML element
            if (elementDescriptor.isElementXmlText(elementIndex)) {
                composer.appendText(value)
                return
            }
        }

        // Value is being encoded as an XML element
        val namespace =
            getElementNamespace(
                elementDescriptor?.getElementXmlNamespace(elementIndex)
                    ?: descriptor.getXmlNamespace(),
                namespaces
            )
        val prefix = getElementNamespacePrefix(namespace, namespaces)
        val name =
            elementDescriptor?.getElementXmlSerialName(elementIndex)
                ?: descriptor.getXmlSerialName()

        composer.startElement(prefix, name)
        if (value.isEmpty()) {
            composer.selfEndElement()
        } else {
            composer.endElementStart().appendText(value).endElement(prefix, name)
        }
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
