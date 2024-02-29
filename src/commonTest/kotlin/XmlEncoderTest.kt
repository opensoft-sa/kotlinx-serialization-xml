@file:OptIn(ExperimentalSerializationApi::class)

package pt.opensoft.kotlinx.serialization.xml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
@SerialName("Greeting")
data class Greeting(
    @XmlAttribute val from: String,
    @XmlAttribute val to: String,
    val message: Message
)

@Serializable @SerialName("Message") data class Message(@XmlText val content: String)

class XmlEncoderTest {
    val xml = Xml { encodeDefaults = true }

    @Test
    fun basic() {
        val actual =
            xml.encodeToString(Greeting(from = "Ryan", to = "Bill", message = Message("Hi")))
        val expected = """<Greeting from="Ryan" to="Bill"><Message>Hi</Message></Greeting>"""
        assertEquals(expected, actual)
    }

    @Test
    fun defaultNamespaces() {
        @Serializable
        @DeclaresXmlNamespace("http://example.com/entity")
        @SerialName("DefaultNamespace")
        data class DefaultNamespace(
            @XmlAttribute val foo: String = "fooz",
            @XmlAttribute val bar: String = "barz",
        )

        val actual = xml.encodeToString(DefaultNamespace("fooz", "barz"))
        val expected =
            """<DefaultNamespace xmlns="http://example.com/entity" foo="fooz" bar="barz"/>"""
        assertEquals(expected, actual)
    }

    @Test
    fun simpleAttributes() {
        @Serializable
        @SerialName("SimpleAttributes")
        data class SimpleAttributes(
            @XmlAttribute val first: String = "string",
            @XmlAttribute val second: Int = 1,
            @XmlAttribute val third: Float = 4.32f,
            @XmlAttribute val fourth: Double = 1.23,
            @XmlAttribute val fifth: Long = 123L,
            @XmlAttribute val sixth: Boolean = false,
            @XmlAttribute val seventh: Boolean = true,
        )

        val actual = xml.encodeToString(SimpleAttributes())
        val expected =
            """<SimpleAttributes first="string" second="1" third="4.32" fourth="1.23" fifth="123" sixth="false" seventh="true"/>"""
        assertEquals(expected, actual)
    }

    @Test
    fun contentEncodedAsText() {
        @Serializable
        @SerialName("ContentAsText")
        data class ContentAsText(
            @XmlAttribute val first: String = "one",
            @XmlAttribute val second: String = "two",
            @XmlText val third: String = "three",
            val fourth: String = "four",
        )

        val actual = xml.encodeToString(ContentAsText())
        val expected =
            """<ContentAsText first="one" second="two">three<fourth>four</fourth></ContentAsText>"""
        assertEquals(expected, actual)
    }

    @Test
    fun encodesDefaultNamespaces() {
        @Serializable
        @SerialName("stream")
        @DeclaresXmlNamespace("jabber:client")
        @XmlNamespace("http://etherx.jabber.org/streams", "stream")
        data class Stream(
            @XmlAttribute val from: String = "me@jabber.im",
            @XmlAttribute val to: String = "jabber.im",
            @XmlAttribute val version: String = "1.0",
            @XmlAttribute val lang: String = "en",
        )

        val actual = xml.encodeToString(Stream())
        val expected =
            """<stream:stream xmlns="jabber:client" xmlns:stream="http://etherx.jabber.org/streams" from="me@jabber.im" to="jabber.im" version="1.0" lang="en"/>"""
        assertEquals(expected, actual)
    }

    @Test
    fun encodesMultipleNamespaces() {
        @Serializable
        @SerialName("stream")
        @DeclaresXmlNamespace("jabber:client")
        @XmlNamespace("http://etherx.jabber.org/streams", "stream")
        data class Stream(
            @XmlNamespace("jabber:client") @XmlAttribute val from: String = "me@jabber.im",
            @XmlNamespace("jabber:client") @XmlAttribute val to: String = "jabber.im",
            @XmlAttribute val version: String = "1.0",
            @XmlAttribute val lang: String = "en",
        )

        val actual = xml.encodeToString(Stream())
        val expected =
            """<stream:stream xmlns="jabber:client" xmlns:stream="http://etherx.jabber.org/streams" from="me@jabber.im" to="jabber.im" version="1.0" lang="en"/>"""
        assertEquals(expected, actual)
    }
}
