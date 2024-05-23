package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest
import pt.opensoft.kotlinx.serialization.xml.util.defaultTestConfig

enum class GreetingType {
    Formal,
    Informal,
    Unspecified
}

@Serializable
data class Greeting(
    @XmlAttribute val from: String,
    @XmlAttribute val to: String,
    val message: Message,
    val type: GreetingType = GreetingType.Unspecified,
    val ps: String? = null
)

@Serializable data class Message(@XmlText val content: String)

class GreetingTest : SerializationTest<Greeting>() {
    override val config: Xml = Xml(defaultTestConfig) { encodeDefaults = false }

    override val serializer = Greeting.serializer()
    override val value =
        Greeting(
            from = "Ryan",
            to = "Bill",
            message = Message("Hi"),
            type = GreetingType.Informal,
            ps = "Bye"
        )
    override val xml: String =
        """
        <Greeting from="Ryan" to="Bill">
            <message>Hi</message>
            <type>Informal</type>
            <ps>Bye</ps>
        </Greeting>
        """
    override val element: XmlElement =
        buildXmlElement("Greeting") {
            addAttribute("from", "Ryan")
            addAttribute("to", "Bill")
            addXmlElement("message") { addText("Hi") }
            addXmlElement("type") { addText("Informal") }
            addXmlElement("ps") { addText("Bye") }
        }
}

@Serializable
data class Greetings(@XmlName("greeting") val greetings: List<Greeting> = emptyList())

class GreetingsTest : SerializationTest<Greetings>() {
    override val config: Xml = Xml(defaultTestConfig) { encodeDefaults = false }

    override val serializer = Greetings.serializer()
    override val value =
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
    override val xml: String =
        """
        <Greetings>
            <greeting from="Ryan" to="Bill">
                <message>Hi</message>
                <type>Informal</type>
                <ps>Bye</ps>
            </greeting>
            <greeting from="Joanne" to="Roger">
                <message>Hey</message>
            </greeting>
        </Greetings>
        """
    override val element: XmlElement =
        buildXmlElement("Greetings") {
            addXmlElement("greeting") {
                addAttribute("from", "Ryan")
                addAttribute("to", "Bill")
                addXmlElement("message") { addText("Hi") }
                addXmlElement("type") { addText("Informal") }
                addXmlElement("ps") { addText("Bye") }
            }
            addXmlElement("greeting") {
                addAttribute("from", "Joanne")
                addAttribute("to", "Roger")
                addXmlElement("message") { addText("Hey") }
            }
        }
}
