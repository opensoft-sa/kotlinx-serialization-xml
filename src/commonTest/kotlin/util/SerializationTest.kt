package pt.opensoft.kotlinx.serialization.xml.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.KSerializer
import pt.opensoft.kotlinx.serialization.xml.Xml
import pt.opensoft.kotlinx.serialization.xml.XmlElement

val defaultTestConfig = Xml {
    prettyPrint = true
    encodeDefaults = true
}

abstract class SerializationTest<T> {
    open val config: Xml = defaultTestConfig

    abstract val serializer: KSerializer<T>
    abstract val value: T
    abstract val xml: String
    abstract val element: XmlElement

    private val trimmedXml by lazy { xml.trimIndent() }

    @Test
    fun testEncodeToString() {
        assertEquals(trimmedXml, config.encodeToString(serializer, value))
    }

//    @Test
//    fun testDecodeFromString() {
//        assertEquals(value, config.decodeFromString(serializer, trimmedXml))
//    }

    @Test
    fun testEncodeToXmlElement() {
        assertEquals(element, config.encodeToXmlElement(serializer, value))
    }

//    @Test
//    fun testDecodeFromXmlElement() {
//        assertEquals(value, config.decodeFromXmlElement(serializer, element))
//    }
}
