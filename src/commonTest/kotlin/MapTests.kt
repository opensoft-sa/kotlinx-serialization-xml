package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest

class TopLevelMapTest : SerializationTest<Map<String, String>>() {
    override val serializer = MapSerializer(String.serializer(), String.serializer())
    override val value = mapOf("a" to "b", "c" to "d")
    override val xml: String =
        """
        <LinkedHashMap>
            <String key="a">b</String>
            <String key="c">d</String>
        </LinkedHashMap>
        """
    override val element: XmlElement =
        buildXmlElement("LinkedHashMap") {
            addXmlElement("String") {
                addAttribute("key", "a")
                addText("b")
            }
            addXmlElement("String") {
                addAttribute("key", "d")
                addText("d")
            }
        }
}

@Serializable
data class ClassWithMap(
    val map: Map<String, Wrapper> = mapOf("a" to Wrapper("b"), "c" to Wrapper("d"))
)

class ClassWithMapTest : SerializationTest<ClassWithMap>() {
    override val serializer = ClassWithMap.serializer()
    override val value = ClassWithMap()
    override val xml: String =
        """
        <ClassWithMap>
            <map key="a" value="b" />
            <map key="c" value="d" />
        </ClassWithMap>
        """
    override val element: XmlElement =
        buildXmlElement("ClassWithMap") {
            addXmlElement("map") {
                addAttribute("key", "a")
                addAttribute("value", "b")
            }
            addXmlElement("map") {
                addAttribute("key", "c")
                addAttribute("value", "d")
            }
        }
}
