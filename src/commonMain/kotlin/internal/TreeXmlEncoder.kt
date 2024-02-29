package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.*

public fun <T> writeXml(xml: Xml, value: T, serializer: SerializationStrategy<T>): XmlElement {
    lateinit var result: XmlElement
    val encoder = XmlTreeEncoder(xml) { result = it }
    encoder.encodeSerializableValue(serializer, value)
    return result
}

private class XmlTreeEncoder(
    override val xml: Xml,
    /** Namespaces in scope by prefix. */
    private val namespaces: Map<String, String> = INITIAL_NAMESPACES_IN_SCOPE,
    private val parentElement: XmlElement? = null,
    private val nodeConsumer: (XmlElement) -> Unit
) : XmlEncoder {
    override val serializersModule: SerializersModule
        get() = xml.serializersModule

    private val configuration = xml.configuration

    private fun addAttributeToParent(attribute: XmlElement.Attribute) {
        parentElement!!.attributes as MutableList += attribute
    }

    private fun addContentToParent(content: XmlContent) {
        parentElement!!.content as MutableList += content
    }

    override fun encodeXmlElement(element: XmlElement) {
        encodeSerializableValue(XmlElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        configuration.encodeDefaults

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val namespacesInScope = namespaces.toMutableMap()
        val attributes: MutableList<XmlElement.Attribute> = mutableListOf()
        val addAttribute: (XmlElement.Attribute) -> Unit = { attributes += it }

        // Declare specified namespaces
        declareSpecifiedNamespaces(descriptor, namespacesInScope, addAttribute)

        // Obtain namespace of the element and declare it needed
        val namespace = getAndDeclareElementNamespace(descriptor, namespacesInScope, addAttribute)

        // Declare children namespaces not in scope
        declareChildrenNamespaces(descriptor, namespacesInScope, addAttribute)

        val element = XmlElement(descriptor.getXmlName(), namespace, attributes, mutableListOf())
        return XmlTreeEncoder(xml, namespacesInScope, element, nodeConsumer)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        nodeConsumer(parentElement!!)
    }

    private fun encodeElement(descriptor: SerialDescriptor, index: Int, value: String) {
        val annotations = descriptor.getElementAnnotations(index)
        val isAttribute = annotations.filterIsInstance<XmlAttribute>().isNotEmpty()
        val isText = annotations.filterIsInstance<XmlText>().isNotEmpty()
        val hasNamespaceDeclaration =
            annotations.filterIsInstance<DeclaresXmlNamespace>().isNotEmpty()

        if (isAttribute && isText) {
            throw XmlSerializationException(
                "Properties cannot have both @XmlAttribute and @XmlText annotations"
            )
        }
        if (isAttribute && hasNamespaceDeclaration) {
            throw XmlSerializationException(
                "Properties cannot have both @XmlAttribute and @XmlNamespaceDeclaration annotations"
            )
        }
        if (isText && hasNamespaceDeclaration) {
            throw XmlSerializationException(
                "Properties cannot have both @XmlText and @XmlNamespaceDeclaration annotations"
            )
        }

        val name = descriptor.getElementXmlName(index)
        if (isAttribute) {
            // If a namespace was specified, it must be in scope, since the parent element makes
            // sure that all children namespaces have been declared; otherwise, by default,
            // attributes have no namespace
            val namespace =
                annotations.filterIsInstance<XmlNamespace>().firstOrNull()?.uri
                    ?: EMPTY_NAMESPACE_URI
            addAttributeToParent(XmlElement.Attribute(name, namespace, value))
            return
        }
        if (isText) {
            addContentToParent(XmlElement.Text(value))
            return
        }

        val namespacesInScope = namespaces.toMutableMap()
        val attributes: MutableList<XmlElement.Attribute> = mutableListOf()
        val addAttribute: (XmlElement.Attribute) -> Unit = { attributes += it }

        // Declare specified namespaces
        declareSpecifiedNamespaces(
            descriptor.getElementDescriptor(index),
            namespacesInScope,
            addAttribute
        )

        // Obtain namespace of the element and declare it if not already in scope
        val namespace =
            getAndDeclareElementNamespace(
                descriptor.getElementDescriptor(index),
                namespacesInScope,
                addAttribute
            )

        addContentToParent(XmlElement(name, namespace, attributes, listOf(XmlElement.Text(value))))
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        encodeSerializableValue(serializer, value)
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value != null) {
            encodeSerializableValue(serializer, value)
        }
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) =
        encodeElement(descriptor, index, value.toString())

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) =
        encodeElement(descriptor, index, value.toString())

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) =
        encodeElement(descriptor, index, value.toString())

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) =
        encodeElement(descriptor, index, value.toString())

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) =
        encodeElement(descriptor, index, value.toString())

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) =
        encodeElement(descriptor, index, value.toString())

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) =
        encodeElement(descriptor, index, value.toString())

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) =
        encodeElement(descriptor, index, value.toString())

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) =
        encodeElement(descriptor, index, value)

    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder = this

    override fun encodeNull() {
        TODO("Not yet implemented")
    }

    override fun encodeBoolean(value: Boolean) {
        TODO("Not yet implemented")
    }

    override fun encodeByte(value: Byte) {
        TODO("Not yet implemented")
    }

    override fun encodeShort(value: Short) {
        TODO("Not yet implemented")
    }

    override fun encodeInt(value: Int) {
        TODO("Not yet implemented")
    }

    override fun encodeLong(value: Long) {
        TODO("Not yet implemented")
    }

    override fun encodeFloat(value: Float) {
        TODO("Not yet implemented")
    }

    override fun encodeDouble(value: Double) {
        TODO("Not yet implemented")
    }

    override fun encodeChar(value: Char) {
        TODO("Not yet implemented")
    }

    override fun encodeString(value: String) {
        TODO("Not yet implemented")
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        TODO("Not yet implemented")
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this
}
