package pt.opensoft.kotlinx.serialization.xml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class XmlEncoderTest {
    @Test
    fun testEncodePrimitives() {
        assertFailsWith<XmlSerializationException> { Xml.encodeToString(null as String?) }
        assertEquals("<Boolean>1</Boolean>", Xml.encodeToString(true))
        assertEquals("<Boolean>true</Boolean>", Xml { prettyPrint = true }.encodeToString(true))
        assertEquals("<Byte>1</Byte>", Xml.encodeToString(1.toByte()))
        assertEquals("<Short>1</Short>", Xml.encodeToString(1.toShort()))
        assertEquals("<Int>1</Int>", Xml.encodeToString(1))
        assertEquals("<Long>1</Long>", Xml.encodeToString(1L))
        assertEquals("<Float>1.0</Float>", Xml.encodeToString(1f))
        assertEquals("<Double>1.0</Double>", Xml.encodeToString(1.0))
        assertEquals("<Char>x</Char>", Xml.encodeToString('x'))
        assertEquals("<String>x</String>", Xml.encodeToString("x"))
        assertEquals("<String/>", Xml.encodeToString(""))
        assertEquals("<GreetingType>Formal</GreetingType>", Xml.encodeToString(GreetingType.Formal))
    }

    @Test
    fun testEncodeList() {
        assertEquals(
            "<ArrayList><Int>1</Int><Int>2</Int></ArrayList>",
            Xml.encodeToString(listOf(1, 2))
        )
    }

    @Test
    fun testEncodeGreeting() {
        val actual =
            Xml.encodeToString(
                Greeting(
                    from = "Ryan",
                    to = "Bill",
                    type = GreetingType.Informal,
                    message = Message("Hi"),
                    ps = "Bye"
                )
            )
        val expected =
            """<Greeting from="Ryan" to="Bill"><message>Hi</message><type>Informal</type><ps>Bye</ps></Greeting>"""
        assertEquals(expected, actual)
    }

    @Test
    fun testEncodeGreetingPrettyPrint() {
        val actual =
            Xml { prettyPrint = true }
                .encodeToString(
                    Greeting(
                        from = "Ryan",
                        to = "Bill",
                        message = Message("Hi"),
                        type = GreetingType.Informal,
                        ps = "Bye"
                    )
                )
        val expected =
            """
            <Greeting from="Ryan" to="Bill">
                <message>Hi</message>
                <type>Informal</type>
                <ps>Bye</ps>
            </Greeting>
            """
                .trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun testEncodeGreetings() {
        val actual =
            Xml.encodeToString(
                Greetings(
                    listOf(
                        Greeting(
                            from = "Ryan",
                            to = "Bill",
                            message = Message("Hi"),
                            type = GreetingType.Informal,
                            ps = "Bye"
                        ),
                        Greeting(from = "Joanne", to = "Roger", message = Message("Hey"))
                    )
                )
            )
        val expected =
            """<Greetings><greetings from="Ryan" to="Bill"><message>Hi</message><type>Informal</type><ps>Bye</ps></greetings><greetings from="Joanne" to="Roger"><message>Hey</message></greetings></Greetings>"""
        assertEquals(expected, actual)
    }

    @Test
    fun testEncodeGreetingsPrettyPrint() {
        val actual =
            Xml { prettyPrint = true }
                .encodeToString(
                    Greetings(
                        listOf(
                            Greeting(
                                from = "Ryan",
                                to = "Bill",
                                message = Message("Hi"),
                                type = GreetingType.Informal,
                                ps = "Bye"
                            ),
                            Greeting(from = "Joanne", to = "Roger", message = Message("Hey"))
                        )
                    )
                )
        val expected =
            """
            <Greetings>
                <greetings from="Ryan" to="Bill">
                    <message>Hi</message>
                    <type>Informal</type>
                    <ps>Bye</ps>
                </greetings>
                <greetings from="Joanne" to="Roger">
                    <message>Hey</message>
                </greetings>
            </Greetings>
            """
                .trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun testEncodeMap() {
        @Serializable data class Wrapper(@XmlAttribute val value: String)

        @Serializable
        data class ClassWithMap(
            val map: Map<String, Wrapper> = mapOf("a" to Wrapper("b"), "c" to Wrapper("d"))
        )

        val actual =
            Xml {
                    encodeDefaults = true
                    prettyPrint = true
                }
                .encodeToString(ClassWithMap())
        val expected =
            """
            <ClassWithMap>
                <map key="a" value="b"/>
                <map key="c" value="d"/>
            </ClassWithMap>
            """
                .trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun testDeclareDefaultNamespace() {
        @DeclaresXmlNamespace("http://example.com")
        @Serializable
        data class DefaultNamespace(@XmlAttribute val x: String)

        val actual = Xml.encodeToString(DefaultNamespace("y"))
        val expected = """<DefaultNamespace xmlns="http://example.com" x="y"/>"""
        assertEquals(expected, actual)
    }

    @Test
    fun testEncodeSimpleAttributes() {
        @Serializable
        data class SimpleAttributes(
            @XmlAttribute val first: String = "string",
            @XmlAttribute val second: Int = 1,
            @XmlAttribute val third: Float = 4.32f,
            @XmlAttribute val fourth: Double = 1.23,
            @XmlAttribute val fifth: Long = 123L,
            @XmlAttribute val sixth: Boolean = false,
            val seventh: Boolean = true,
        )

        val actual = Xml { encodeDefaults = true }.encodeToString(SimpleAttributes())
        val expected =
            """<SimpleAttributes first="string" second="1" third="4.32" fourth="1.23" fifth="123" sixth="false"><seventh>true</seventh></SimpleAttributes>"""
        assertEquals(expected, actual)
    }

    @Test
    fun testEncodeContentAsText() {
        @Serializable
        data class ContentAsText(
            @XmlAttribute val first: String = "one",
            @XmlAttribute val second: String = "two",
            @XmlText val third: String = "three",
            val fourth: String = "four",
        )

        val actual = Xml { encodeDefaults = true }.encodeToString(ContentAsText())
        val expected =
            """<ContentAsText first="one" second="two">three<fourth>four</fourth></ContentAsText>"""
        assertEquals(expected, actual)
    }

    @Test
    fun testEncodeContentAsTextPrettyPrint() {
        @Serializable
        data class ContentAsText(
            @XmlAttribute val first: String = "one",
            @XmlAttribute val second: String = "two",
            @XmlText val third: String = "three",
            val fourth: String = "four",
        )

        val actual =
            Xml {
                    encodeDefaults = true
                    prettyPrint = true
                }
                .encodeToString(ContentAsText())
        val expected =
            """
            <ContentAsText first="one" second="two">three<fourth>four</fourth>
            </ContentAsText>
            """
                .trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun encodesDefaultNamespaces() {
        @Serializable
        @XmlName("stream")
        @DeclaresXmlNamespace("jabber:client")
        @XmlNamespace("http://etherx.jabber.org/streams", "stream")
        data class Stream(
            @XmlAttribute val from: String = "me@jabber.im",
            @XmlAttribute val to: String = "jabber.im",
            @XmlAttribute val version: String = "1.0",
            @XmlAttribute val lang: String = "en",
        )

        val actual = Xml { encodeDefaults = true }.encodeToString(Stream())
        val expected =
            """<stream:stream xmlns="jabber:client" xmlns:stream="http://etherx.jabber.org/streams" from="me@jabber.im" to="jabber.im" version="1.0" lang="en"/>"""
        assertEquals(expected, actual)
    }

    @Test
    fun encodesMultipleNamespaces() {
        @Serializable
        @XmlName("stream")
        @DeclaresXmlNamespace("jabber:client")
        @XmlNamespace("http://etherx.jabber.org/streams", "stream")
        data class Stream(
            @XmlNamespace("jabber:client", "jc") @XmlAttribute val from: String = "me@jabber.im",
            @XmlNamespace("jabber:client", "jc") @XmlAttribute val to: String = "jabber.im",
            @XmlAttribute val version: String = "1.0",
            @XmlAttribute val lang: String = "en",
        )

        val actual = Xml { encodeDefaults = true }.encodeToString(Stream())
        val expected =
            """<stream:stream xmlns="jabber:client" xmlns:stream="http://etherx.jabber.org/streams" xmlns:jc="jabber:client" jc:from="me@jabber.im" jc:to="jabber.im" version="1.0" lang="en"/>"""
        assertEquals(expected, actual)
    }

    @Test
    fun testEncodeWrap() {
        @Serializable data class Box(@XmlText val content: String)

        @Serializable
        data class Wrapper(
            val box: Box = Box("a"),
            @XmlWrap val wrapBox: Box = Box("b"),
            val boxList: List<Box> = listOf(Box("c"), Box("d")),
            @XmlWrap val wrapBoxList: List<Box> = listOf(Box("e"), Box("f"))
        )

        val actual =
            Xml {
                    encodeDefaults = true
                    prettyPrint = true
                }
                .encodeToString(Wrapper())
        val expected =
            """
            <Wrapper>
                <box>a</box>
                <wrapBox>
                    <Box>b</Box>
                </wrapBox>
                <boxList>c</boxList>
                <boxList>d</boxList>
                <wrapBoxList>
                    <Box>e</Box>
                    <Box>f</Box>
                </wrapBoxList>
            </Wrapper>
            """
                .trimIndent()
        assertEquals(expected, actual)
    }
}
