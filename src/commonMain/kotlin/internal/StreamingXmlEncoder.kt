package pt.opensoft.kotlinx.serialization.xml.internal

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.*

internal class StreamingXmlEncoder(
    override val xml: Xml,
    private val composer: Composer,
    /** Namespaces in scope by prefix. */
    private val namespaces: Map<String, String> = INITIAL_NAMESPACES_IN_SCOPE,
    private val parentComposer: Composer? = null,
) : XmlEncoder {
    override val serializersModule: SerializersModule = xml.serializersModule

    private val configuration = xml.configuration

    override fun encodeXmlElement(element: XmlElement) {
        encodeSerializableValue(XmlElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        configuration.encodeDefaults

    private fun prefixedName(prefix: String, name: String): String =
        if (prefix.isEmpty()) name else "$prefix:$name"

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val namespacesInScope = namespaces.toMutableMap()
        val attributesComposer = Composer(composer)
        val addAttribute: (XmlElement.Attribute) -> Unit = {
            attributesComposer.newAttribute().append(it.name).append("=").appendQuoted(it.value)
        }

        // Declare specified namespaces
        declareSpecifiedNamespaces(descriptor, namespacesInScope, addAttribute)

        // Obtain namespace of the element and declare it if not already in scope
        val namespace = getAndDeclareElementNamespace(descriptor, namespacesInScope, addAttribute)
        val prefix = getElementNamespacePrefix(namespace, namespacesInScope)

        // Declare children namespaces not in scope
        declareChildrenNamespaces(descriptor, namespacesInScope, addAttribute)

        composer
            .indent()
            .newElement()
            .append("<")
            .append(prefixedName(prefix, descriptor.getXmlName()))
            .append(attributesComposer)

        val contentComposer = Composer(composer).indent()
        return StreamingXmlEncoder(xml, contentComposer, namespacesInScope, composer)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (composer.sb.isEmpty()) {
            parentComposer!!.append("/>").unIndent()
            return
        }

        val namespace = getElementNamespace(descriptor, namespaces)
        val prefix = getElementNamespacePrefix(namespace, namespaces)
        parentComposer!!
            .append(">")
            .appendLine()
            .append(composer)
            .appendLine()
            .append("</")
            .append(prefixedName(prefix, descriptor.getXmlName()))
            .append(">")
            .unIndent()
    }

    private fun encodeElement(descriptor: SerialDescriptor, index: Int, value: Any) {
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
            parentComposer!!.newAttribute()
            // If a namespace was specified, it must be in scope, since the parent element makes
            // sure that all children namespaces have been declared; otherwise, by default,
            // attributes have no namespace
            val namespace =
                annotations.filterIsInstance<XmlNamespace>().firstOrNull()?.uri
                    ?: EMPTY_NAMESPACE_URI
            val prefix = getAttributeNamespacePrefix(namespace, namespaces)
            parentComposer
                .append(prefixedName(prefix, name))
                .append("=")
                .appendQuoted(value.toString())
            return
        }
        if (isText) {
            composer.append(value.toString())
            return
        }

        val namespacesInScope = namespaces.toMutableMap()
        val attributesComposer = Composer(composer)
        val addAttribute: (XmlElement.Attribute) -> Unit = {
            attributesComposer.newAttribute().append(it.name).append("=").appendQuoted(it.value)
        }

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
        val prefix = getElementNamespacePrefix(namespace, namespacesInScope)
        val tag = prefixedName(prefix, name)

        composer
            .append("<")
            .append(tag)
            .append(attributesComposer)
            .append(">")
            .append(value.toString())
            .append("</")
            .append(tag)
            .append(">")
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
