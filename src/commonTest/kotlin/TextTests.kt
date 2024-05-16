package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest

@Serializable
data class ContentAsText(
    @XmlAttribute val first: String = "one",
    @XmlAttribute val second: String = "two",
    @XmlText val third: String = "three",
    val fourth: String = "four",
)

class TextTest : SerializationTest<ContentAsText>() {
    override val serializer = ContentAsText.serializer()
    override val value = ContentAsText()
    override val xml: String =
        """
        <ContentAsText first="one" second="two">three<fourth>four</fourth>
        </ContentAsText>
        """
    override val element: XmlElement =
        buildXmlElement("ContentAsText") {
            addAttribute("first", "one")
            addAttribute("second", "two")
            addText("three")
            addXmlElement("fourth") { addText("four") }
        }
}
