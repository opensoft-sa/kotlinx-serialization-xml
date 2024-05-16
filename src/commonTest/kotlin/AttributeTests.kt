package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest

@Serializable
data class PrimitiveAttributes(
    @XmlAttribute val boolean: Boolean = false,
    @XmlAttribute val byte: Byte = 1,
    @XmlAttribute val short: Short = 1,
    @XmlAttribute val int: Int = 1,
    @XmlAttribute val long: Long = 1L,
    @XmlAttribute val float: Float = 1f,
    @XmlAttribute val double: Double = 1.0,
    @XmlAttribute val char: Char = 'x',
    @XmlAttribute val string: String = "x",
    @XmlAttribute val enum: Enum = Enum.V1
)

class PrimitiveAttributesTest : SerializationTest<PrimitiveAttributes>() {
    override val serializer = PrimitiveAttributes.serializer()
    override val value = PrimitiveAttributes()
    override val xml: String =
        """<PrimitiveAttributes boolean="false" byte="1" short="1" int="1" long="1" float="1.0" double="1.0" char="x" string="x" enum="V1" />"""
    override val element: XmlElement =
        buildXmlElement("PrimitiveAttributes") {
            addAttribute("boolean", "false")
            addAttribute("byte", "1")
            addAttribute("short", "1")
            addAttribute("int", "1")
            addAttribute("long", "1")
            addAttribute("float", "1.0")
            addAttribute("double", "1.0")
            addAttribute("char", "x")
            addAttribute("string", "x")
            addAttribute("enum", "V1")
        }
}

@Serializable
data class NamedAttributes(
    @XmlAttribute @XmlName("n") val named: String = "x",
    @XmlAttribute @XmlNamespace("http://example.com", "ex") val namespaced: String = "y",
    @XmlAttribute
    @XmlName("nn")
    @XmlNamespace("http://example.com", "ex")
    val namedAndNamespaced: String = "z",
)

class NamedAttributesTest : SerializationTest<NamedAttributes>() {
    override val serializer = NamedAttributes.serializer()
    override val value = NamedAttributes()
    override val xml: String =
        """<NamedAttributes xmlns:ex="http://example.com" n="x" ex:namespaced="y" ex:nn="z" />"""
    override val element: XmlElement =
        buildXmlElement("NamedAttributes") {
            declareNamespace("http://example.com", "ex")
            addAttribute("n", "x")
            addAttribute("namespaced", "http://example.com", "y")
            addAttribute("nn", "http://example.com", "z")
        }
}
