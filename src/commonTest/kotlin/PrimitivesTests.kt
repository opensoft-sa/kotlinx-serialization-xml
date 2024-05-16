package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest
import pt.opensoft.kotlinx.serialization.xml.util.defaultTestConfig

class PrettyBooleanTest : SerializationTest<Boolean>() {
    override val serializer = Boolean.serializer()
    override val value = true
    override val xml: String = "<Boolean>true</Boolean>"
    override val element: XmlElement = buildXmlElement("Boolean") { addText("true") }
}

class UglyBooleanTest : SerializationTest<Boolean>() {
    override val config = Xml(defaultTestConfig) { prettyPrint = false }

    override val serializer = Boolean.serializer()
    override val value = true
    override val xml: String = "<Boolean>1</Boolean>"
    override val element: XmlElement = buildXmlElement("Boolean") { addText("1") }
}

class ByteTest : SerializationTest<Byte>() {
    override val serializer = Byte.serializer()
    override val value = 1.toByte()
    override val xml: String = "<Byte>1</Byte>"
    override val element: XmlElement = buildXmlElement("Byte") { addText("1") }
}

class ShortTest : SerializationTest<Short>() {
    override val serializer = Short.serializer()
    override val value = 1.toShort()
    override val xml: String = "<Short>1</Short>"
    override val element: XmlElement = buildXmlElement("Short") { addText("1") }
}

class IntTest : SerializationTest<Int>() {
    override val serializer = Int.serializer()
    override val value = 1
    override val xml: String = "<Int>1</Int>"
    override val element: XmlElement = buildXmlElement("Int") { addText("1") }
}

class LongTest : SerializationTest<Long>() {
    override val serializer = Long.serializer()
    override val value = 1L
    override val xml: String = "<Long>1</Long>"
    override val element: XmlElement = buildXmlElement("Long") { addText("1") }
}

class FloatTest : SerializationTest<Float>() {
    override val serializer = Float.serializer()
    override val value = 1f
    override val xml: String = "<Float>1.0</Float>"
    override val element: XmlElement = buildXmlElement("Float") { addText("1.0") }
}

class DoubleTest : SerializationTest<Double>() {
    override val serializer = Double.serializer()
    override val value = 1.0
    override val xml: String = "<Double>1.0</Double>"
    override val element: XmlElement = buildXmlElement("Double") { addText("1.0") }
}

class CharTest : SerializationTest<Char>() {
    override val serializer = Char.serializer()
    override val value = 'x'
    override val xml: String = "<Char>x</Char>"
    override val element: XmlElement = buildXmlElement("Char") { addText("x") }
}

class StringTest : SerializationTest<String>() {
    override val serializer = String.serializer()
    override val value = "x"
    override val xml: String = "<String>x</String>"
    override val element: XmlElement = buildXmlElement("String") { addText("x") }
}

class PrettyEmptyStringTest : SerializationTest<String>() {
    override val serializer = String.serializer()
    override val value = ""
    override val xml: String = "<String />"
    override val element: XmlElement = buildXmlElement("String")
}

class UglyEmptyStringTest : SerializationTest<String>() {
    override val config = Xml(defaultTestConfig) { prettyPrint = false }

    override val serializer = String.serializer()
    override val value = ""
    override val xml: String = "<String/>"
    override val element: XmlElement = buildXmlElement("String")
}

@Serializable
enum class Enum {
    V1,
    V2
}

class EnumTest : SerializationTest<Enum>() {
    override val serializer = Enum.serializer()
    override val value = Enum.V1
    override val xml: String = "<Enum>V1</Enum>"
    override val element: XmlElement = buildXmlElement("Enum") { addText("V1") }
}
