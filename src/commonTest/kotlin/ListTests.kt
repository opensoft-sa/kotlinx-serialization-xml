package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest

class TopLevelListTest : SerializationTest<List<Int>>() {
    override val serializer = ListSerializer(Int.serializer())
    override val value = listOf(1, 2)
    override val xml: String =
        """
        <ArrayList>
            <Int>1</Int>
            <Int>2</Int>
        </ArrayList>
        """
    override val element: XmlElement =
        buildXmlElement("ArrayList") {
            addXmlElement("Int") { addText("1") }
            addXmlElement("Int") { addText("2") }
        }
}
