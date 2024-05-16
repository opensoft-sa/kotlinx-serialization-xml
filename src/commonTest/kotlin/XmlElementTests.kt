package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest

@Serializable
data class ClassWithXmlElement(
    @XmlAttribute val attr: Int = 1,
    val text: String = "x",
    val flatElement: XmlElement =
        buildXmlElement("flatElement") { // This name is ignored due to the flattening
            addAttribute("a", "b")
            addText("c")
        },
    @XmlWrap
    val wrapElement: XmlElement =
        buildXmlElement("element") {
            addAttribute("d", "e")
            addText("f")
        }
)

class XmlElementEncoderTest : SerializationTest<ClassWithXmlElement>() {
    override val serializer = ClassWithXmlElement.serializer()
    override val value = ClassWithXmlElement()
    override val xml: String =
        """
        <ClassWithXmlElement attr="1">
            <text>x</text>
            <flatElement a="b">c</flatElement>
            <wrapElement>
                <element d="e">f</element>
            </wrapElement>
        </ClassWithXmlElement>
        """
    override val element: XmlElement =
        buildXmlElement("ClassWithXmlElement") {
            addAttribute("attr", "1")
            addXmlElement("text") { addText("x") }
            addXmlElement("flatElement") {
                addAttribute("a", "b")
                addText("c")
            }
            addXmlElement("wrapElement") {
                addXmlElement("element") {
                    addAttribute("d", "e")
                    addText("f")
                }
            }
        }
}
