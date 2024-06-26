package pt.opensoft.kotlinx.serialization.xml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class XmlDecoderTest {
    @Serializable
    data class NamespacedGreetings(
        @SerialName("greeting") val myGreeting: MyGreeting,
        @XmlName("greeting")
        @XmlNamespace("http://greetings.example.com/schema", "")
        val otherGreeting: OtherGreeting,
    )

    @Serializable data class MyGreeting(@XmlText val message: String)

    @Serializable data class OtherGreeting(@XmlText val message: String)

    @Test
    fun withElementNamespaces() {
        val xml =
            """
            <NamespacedGreetings xmlns:other="http://greetings.example.com/schema">
                <greeting>No namespaces here!</greeting>
                <other:greeting>Who is this?</other:greeting>
            </NamespacedGreetings>
            """
                .trimIndent()
        val actual = Xml.decodeFromString<NamespacedGreetings>(xml)
        assertEquals(
            NamespacedGreetings(
                MyGreeting("No namespaces here!"),
                OtherGreeting("Who is this?"),
            ),
            actual
        )
    }

    @Test
    fun undefinedNamespaces() {
        val xml =
            """
            <NamespacedGreetings>
                <greeting>No namespaces here!</greeting>
                <other:greeting>Who is this?</other:greeting>
            </NamespacedGreetings>
            """
                .trimIndent()
        try {
            Xml.decodeFromString<NamespacedGreetings>(xml)
            fail("Expected UndefinedNamespaceException")
        } catch (e: UndeclaredNamespacePrefixException) {
            // success
        }
    }

    @Serializable
    data class Attributes(
        val unannotated: String,
        @XmlAttribute val unnamed: String,
        @XmlAttribute @XmlName("namedAttribute") val named: String,
        @XmlAttribute
        @XmlName("namedNamespaced")
        @XmlNamespace("http://greetings.example.com/schema", "")
        val namedAndNamespaced: String,
        @XmlAttribute
        @XmlNamespace("http://greetings.example.com/schema", "")
        val namespaced: String,
        @XmlNamespace("http://greetings.example.com/schema", "") val onlyNamespaced: String,
    )

    @Test
    fun attributeNamespaces() {
        val xml =
            """
            <Attributes xmlns:ns="http://greetings.example.com/schema"
                unannotated="first"
                unnamed="second"
                namedAttribute="third"
                ns:namedNamespaced="fourth"
                ns:namespaced="fifth"
                ns:onlyNamespaced="sixth" />
            """
                .trimIndent()
        val actual = Xml.decodeFromString<Attributes>(xml)
        assertEquals(
            Attributes(
                "first",
                "second",
                "third",
                "fourth",
                "fifth",
                "sixth",
            ),
            actual
        )
    }

    @Test
    fun namespacedAttributeBeforeNamespaceDecl() {
        val xml =
            """
            <Attributes
                unannotated="first"
                unnamed="second"
                namedAttribute="third"
                ns:namedNamespaced="fourth"
                ns:namespaced="fifth"
                ns:onlyNamespaced="sixth"
                xmlns:ns="http://greetings.example.com/schema" />
            """
                .trimIndent()
        val actual = Xml.decodeFromString<Attributes>(xml)
        assertEquals(
            Attributes(
                "first",
                "second",
                "third",
                "fourth",
                "fifth",
                "sixth",
            ),
            actual
        )
    }

    @Serializable
    @XmlNamespace("http://etherx.jabber.org/streams", "")
    data class Stream(val from: String, val to: String)

    @Test
    fun namespacedElementContainingNamespaceDecl() {
        val xml =
            """
            <stream:stream
                from="source@xmpp.org"
                to="dest@xmpp.org"
                xmlns:stream="http://etherx.jabber.org/streams" />
            """
                .trimIndent()
        val actual = Xml.decodeFromString<Stream>(xml)
        assertEquals(Stream(from = "source@xmpp.org", to = "dest@xmpp.org"), actual)
    }

    @Serializable data class StreamHolder(val stream: Stream)

    @Test
    fun embeddedNamespacedElementContainingNamespaceDecl() {
        val xml =
            """
            <StreamHolder>
                <stream:stream
                    from="source@xmpp.org"
                    to="dest@xmpp.org"
                    xmlns:stream="http://etherx.jabber.org/streams" />
            </StreamHolder>
            """
                .trimIndent()
        val actual = Xml.decodeFromString<StreamHolder>(xml)
        assertEquals(StreamHolder(Stream(from = "source@xmpp.org", to = "dest@xmpp.org")), actual)
    }

    @Test
    fun skipsComments() {
        val xml =
            """
            <!-- This is some fiiine XML! -->
            <Greeting from="Ryan" to="Bill">
                <!-- In here's not so bad! -->
                <message>Hi</message>
            </Greeting>
            """
                .trimIndent()
        val actual = Xml.decodeFromString<Greeting>(xml)
        assertEquals(Greeting(from = "Ryan", to = "Bill", message = Message("Hi")), actual)
    }

    @Test
    fun skipsXmlDecl() {
        val xml =
            """
            <?xml version="1.1"?>
            <Greeting from="Ryan" to="Bill">
                <message>Hi</message>
            </Greeting>
            """
                .trimIndent()
        val actual = Xml.decodeFromString<Greeting>(xml)
        assertEquals(Greeting(from = "Ryan", to = "Bill", message = Message("Hi")), actual)
    }
}
