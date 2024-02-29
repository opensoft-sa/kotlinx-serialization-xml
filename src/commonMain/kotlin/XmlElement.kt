package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

/** Object allowed as content of an XML element. */
public sealed interface XmlContent

/** Representation of an XML element. */
@Serializable(with = XmlElementSerializer::class)
public class XmlElement(
    public val name: String,
    public val namespace: String = "",
    public val attributes: List<Attribute> = emptyList(),
    public val content: List<XmlContent> = emptyList(),
) : XmlContent, SerialDescriptor {
    init {
        if (name.any { it.isWhitespace() }) {
            throw IllegalArgumentException("XML element names must not contain whitespaces")
        }
    }

    override val serialName: String
        get() = name

    @ExperimentalSerializationApi override val kind: SerialKind = StructureKind.CLASS

    @ExperimentalSerializationApi
    override val elementsCount: Int
        get() = TODO("Not yet implemented")

    @ExperimentalSerializationApi
    override fun getElementAnnotations(index: Int): List<Annotation> {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun getElementDescriptor(index: Int): SerialDescriptor {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun getElementIndex(name: String): Int {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun getElementName(index: Int): String {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun isElementOptional(index: Int): Boolean {
        TODO("Not yet implemented")
    }

    public constructor(
        name: String,
        attributes: List<Attribute> = emptyList(),
        content: List<XmlContent> = emptyList()
    ) : this(name, "", attributes, content)

    /** Attribute of an XML element. */
    public class Attribute(
        public val name: String,
        public val namespace: String = "",
        public val value: String
    ) {
        init {
            if (name.any { it.isWhitespace() }) {
                throw IllegalArgumentException(
                    "XML element attribute names must not contain whitespaces"
                )
            }
        }

        public constructor(name: String, value: String) : this(name, "", value)
    }

    /** Text within an XML element. */
    public class Text(public val content: String) : XmlContent, SerialDescriptor {
        override fun equals(other: Any?): Boolean =
            when {
                this === other -> true
                other !is Text -> false
                content != other.content -> false
                else -> true
            }

        override fun hashCode(): Int = content.hashCode()

        override fun toString(): String = "XmlElement.Text(content=$content)"

        @ExperimentalSerializationApi
        override val serialName: String = "pt.opensoft.kotlinx.serialization.xml.XmlElement.Text"

        @ExperimentalSerializationApi override val kind: SerialKind = PrimitiveKind.STRING

        @ExperimentalSerializationApi override val elementsCount: Int = 0
        override val annotations: List<Annotation> = ANNOTATIONS

        @ExperimentalSerializationApi
        override fun getElementAnnotations(index: Int): List<Annotation> = throwNoSuchElement()

        @ExperimentalSerializationApi
        override fun getElementDescriptor(index: Int): SerialDescriptor = throwNoSuchElement()

        @ExperimentalSerializationApi
        override fun getElementIndex(name: String): Int = throwNoSuchElement()

        @ExperimentalSerializationApi
        override fun getElementName(index: Int): String = throwNoSuchElement()

        @ExperimentalSerializationApi
        override fun isElementOptional(index: Int): Boolean = throwNoSuchElement()

        private fun throwNoSuchElement(): Nothing =
            throw NoSuchElementException("XmlElement.Text has no")

        private companion object {
            private val ANNOTATIONS = listOf(XmlText())
        }
    }
}
