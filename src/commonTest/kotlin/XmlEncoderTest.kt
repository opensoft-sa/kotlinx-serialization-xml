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
                <map key="a" value="b" />
                <map key="c" value="d" />
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
            """<SimpleAttributes first="string" second="1" third="4.32" fourth="1.23" fifth="123" sixth="0"><seventh>1</seventh></SimpleAttributes>"""
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
    fun testEncodeElementWrapping() {
        @Serializable data class Wrapper(@XmlAttribute val text: String)

        @Serializable
        data class ElementWrapping(
            val flatString: String = "x",
            @XmlWrap val wrapString: String = "x",
            val flatClass: Wrapper = Wrapper("x"),
            @XmlWrap val wrapClass: Wrapper = Wrapper("x"),
            val flatStringList: List<String> = listOf("x", "y"),
            @XmlWrap val wrapStringList: List<String> = listOf("x", "y"),
            val flatClassList: List<Wrapper> = listOf(Wrapper("x"), Wrapper("y")),
            @XmlWrap val wrapClassList: List<Wrapper> = listOf(Wrapper("x"), Wrapper("y")),
            val flatStringListList: List<List<String>> = listOf(listOf("x", "y"), listOf("w", "z")),
            @XmlWrap
            val wrapStringListList: List<List<String>> = listOf(listOf("x", "y"), listOf("w", "z")),
            val flatClassListList: List<List<Wrapper>> =
                listOf(listOf(Wrapper("x"), Wrapper("y")), listOf(Wrapper("w"), Wrapper("z"))),
            @XmlWrap
            val wrapClassListList: List<List<Wrapper>> =
                listOf(listOf(Wrapper("x"), Wrapper("y")), listOf(Wrapper("w"), Wrapper("z")))
        )

        val actual =
            Xml {
                    encodeDefaults = true
                    prettyPrint = true
                }
                .encodeToString(ElementWrapping())
        val expected =
            """
            <ElementWrapping>
                <flatString>x</flatString>
                <wrapString>
                    <String>x</String>
                </wrapString>
                <flatClass text="x" />
                <wrapClass>
                    <Wrapper text="x" />
                </wrapClass>
                <flatStringList>x</flatStringList>
                <flatStringList>y</flatStringList>
                <wrapStringList>
                    <String>x</String>
                    <String>y</String>
                </wrapStringList>
                <flatClassList text="x" />
                <flatClassList text="y" />
                <wrapClassList>
                    <Wrapper text="x" />
                    <Wrapper text="y" />
                </wrapClassList>
                <flatStringListList>
                    <String>x</String>
                    <String>y</String>
                </flatStringListList>
                <flatStringListList>
                    <String>w</String>
                    <String>z</String>
                </flatStringListList>
                <wrapStringListList>
                    <ArrayList>
                        <String>x</String>
                        <String>y</String>
                    </ArrayList>
                    <ArrayList>
                        <String>w</String>
                        <String>z</String>
                    </ArrayList>
                </wrapStringListList>
                <flatClassListList>
                    <Wrapper text="x" />
                    <Wrapper text="y" />
                </flatClassListList>
                <flatClassListList>
                    <Wrapper text="w" />
                    <Wrapper text="z" />
                </flatClassListList>
                <wrapClassListList>
                    <ArrayList>
                        <Wrapper text="x" />
                        <Wrapper text="y" />
                    </ArrayList>
                    <ArrayList>
                        <Wrapper text="w" />
                        <Wrapper text="z" />
                    </ArrayList>
                </wrapClassListList>
            </ElementWrapping>
            """
                .trimIndent()
        assertEquals(expected, actual)
    }
}
