package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.*

internal class TreeXmlEncoder(
    override val xml: Xml,
    override val namespaces: Map<String, String> = GLOBAL_NAMESPACES,
    private val parentElement: XmlElement? = null
) : XmlEncoder {
    lateinit var rootElement: XmlElement

    private var elementDescriptor: SerialDescriptor? = null
    private var elementIndex: Int = -1
    private var wrappedElementDescriptor: SerialDescriptor? = null
    private var wrappedElementIndex: Int = -1
    private var flattenStructure: Boolean = false

    override val serializersModule: SerializersModule
        get() = xml.serializersModule

    private val configuration = xml.configuration

    private fun copy(
        namespaces: Map<String, String> = this.namespaces,
        parentElement: XmlElement? = this.parentElement
    ) = TreeXmlEncoder(xml, namespaces, parentElement)

    private fun addContentToParent(content: XmlContent) {
        if (parentElement != null) {
            parentElement.content as MutableList += content
        } else {
            rootElement = content as XmlElement
        }
    }

    private fun addAttributeToParent(attribute: XmlElement.Attribute) {
        parentElement!!.attributes as MutableSet += attribute
    }

    override fun encodeXmlElement(element: XmlElement) {
        val toEncode =
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
        addContentToParent(toEncode)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        configuration.encodeDefaults

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (flattenStructure) {
            val contentEncoder = copy()
            contentEncoder.elementDescriptor = elementDescriptor
            contentEncoder.elementIndex = elementIndex
            contentEncoder.wrappedElementDescriptor = wrappedElementDescriptor
            contentEncoder.wrappedElementIndex = wrappedElementIndex
            return contentEncoder
        }

        descriptor.validateXmlAnnotations()
        descriptor.validateElementXmlAnnotations()

        val namespacesInScope = namespaces.toMutableMap()
        val attributes: MutableSet<XmlElement.Attribute> = mutableSetOf()
        val declareNamespace: (XmlElement.Attribute) -> Unit = { attributes += it }

        // Declare specified namespaces
        declareSpecifiedNamespaces(
            descriptor.getXmlNamespaceDeclarations(),
            namespacesInScope,
            declareNamespace
        )

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
                namespacesInScope,
                declareNamespace
            )

        // Declare children namespaces not in scope
        declareChildrenNamespaces(descriptor, namespacesInScope, declareNamespace)

        val element =
            XmlElement(
                wrappedElementDescriptor?.getElementXmlWrappedName(wrappedElementIndex)
                    ?: elementDescriptor?.getElementXmlName(elementIndex)
                    ?: descriptor.getXmlName(),
                namespace,
                attributes,
                mutableListOf()
            )
        addContentToParent(element)
        return copy(namespaces = namespacesInScope, parentElement = element)
    }

    override fun endStructure(descriptor: SerialDescriptor) {}

    private fun encodeElement(
        descriptor: SerialDescriptor,
        index: Int,
        encodeValue: (encoder: TreeXmlEncoder) -> Unit
    ) {
        // Encoding of a list or map item
        if (
            descriptor.kind == StructureKind.LIST ||
                (descriptor.kind == StructureKind.MAP && index % 2 == 1)
        ) {
            encodeValue(this)
            return
        }

        val elementKind =
            descriptor.getElementDescriptor(index).actualDescriptor(serializersModule).kind
        val isCollection = elementKind == StructureKind.LIST || elementKind == StructureKind.MAP

        // Whether element should be wrapped
        if (descriptor.isElementXmlWrap(index)) {
            val namespace =
                getElementNamespace(descriptor.getElementXmlNamespace(index)?.uri, namespaces)
            val name = descriptor.getElementXmlName(index)

            val element = XmlElement(name, namespace, mutableSetOf(), mutableListOf())
            addContentToParent(element)
            val contentEncoder = copy(parentElement = element)
            contentEncoder.wrappedElementDescriptor = descriptor
            contentEncoder.wrappedElementIndex = index
            contentEncoder.flattenStructure = isCollection
            encodeValue(contentEncoder)
        } else {
            elementDescriptor = descriptor
            elementIndex = index
            flattenStructure = isCollection
            encodeValue(this)
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
        // Value is being encoded as the attribute of an XML element
        if (elementDescriptor?.isElementXmlAttribute(elementIndex) == true) {
            val namespace =
                elementDescriptor!!.getElementXmlNamespace(elementIndex)?.uri ?: NO_NAMESPACE_URI
            val name = elementDescriptor!!.getElementXmlName(elementIndex)
            addAttributeToParent(XmlElement.Attribute(name, namespace, value))
            return
        }

        // Value is being encoded as the text of an XML element
        if (elementDescriptor?.isElementXmlText(elementIndex) == true) {
            addContentToParent(XmlElement.Text(value))
            return
        }

        // Value is being encoded as an XML element
        val namespace =
            getElementNamespace(
                wrappedElementDescriptor?.getElementXmlWrappedNamespace(wrappedElementIndex)?.uri
                    ?: elementDescriptor?.getElementXmlNamespace(elementIndex)?.uri
                    ?: descriptor.getXmlNamespace()?.uri,
                namespaces
            )
        val name =
            wrappedElementDescriptor?.getElementXmlWrappedName(wrappedElementIndex)
                ?: elementDescriptor?.getElementXmlName(elementIndex)
                ?: descriptor.getXmlName()

        addContentToParent(
            XmlElement(
                name,
                namespace,
                if (value.isEmpty()) emptyList() else listOf(XmlElement.Text(value))
            )
        )
    }

    override fun encodeNull() {
        if (parentElement == null) {
            throw XmlException("Root element must not be null.")
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
        encodeValue(enumDescriptor, enumDescriptor.getElementXmlName(index))

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this
}
