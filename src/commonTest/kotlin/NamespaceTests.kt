package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest

@DeclaresXmlNamespace("http://example.com")
@Serializable
data class DefaultNamespace(@XmlAttribute val attr: String = "x")

class DefaultNamespaceTest : SerializationTest<DefaultNamespace>() {
    override val serializer = DefaultNamespace.serializer()
    override val value = DefaultNamespace()
    override val xml: String = """<DefaultNamespace xmlns="http://example.com" attr="x" />"""
    override val element: XmlElement =
        buildXmlElement("DefaultNamespace", "http://example.com") {
            declareNamespace("http://example.com")
            addAttribute("attr", "x")
        }
}

@XmlName("multi")
@XmlNamespace("http://example2.com")
@DeclaresXmlNamespace("http://example.com")
@DeclaresXmlNamespace("http://example2.com", "ex2")
@Serializable
data class MultipleNamespaces(@XmlAttribute val attr: String = "x", val el: String = "y")

class MultipleNamespaceTest : SerializationTest<MultipleNamespaces>() {
    override val serializer = MultipleNamespaces.serializer()
    override val value = MultipleNamespaces()
    override val xml: String =
        """
        <ex2:multi xmlns="http://example.com" xmlns:ex2="http://example2.com" attr="x">
            <el>y</el>
        </ex2:multi>
        """
    override val element: XmlElement =
        buildXmlElement("multi", "http://example2.com") {
            declareNamespace("http://example.com")
            declareNamespace("http://example2.com", "ex2")
            addAttribute("attr", "x")
            addXmlElement("el", "http://example.com") { addText("y") }
        }
}

@DeclaresXmlNamespace("http://example.com")
@DeclaresXmlNamespace("http://example2.com", "ex2")
@Serializable
data class NamespacedAttributes(
    @XmlAttribute @XmlNamespace("http://example.com", "ex") val attr1: String = "x",
    @XmlAttribute @XmlNamespace("http://example2.com") val attr2: String = "y"
)

class NamespacedAttributesTest : SerializationTest<NamespacedAttributes>() {
    override val serializer = NamespacedAttributes.serializer()
    override val value = NamespacedAttributes()
    override val xml: String =
        """<NamespacedAttributes xmlns="http://example.com" xmlns:ex2="http://example2.com" xmlns:ex="http://example.com" ex:attr1="x" ex2:attr2="y" />"""
    override val element: XmlElement =
        buildXmlElement("NamespacedAttributes", "http://example.com") {
            declareNamespace("http://example.com")
            declareNamespace("http://example2.com", "ex2")
            declareNamespace("http://example.com", "ex")
            addAttribute("attr1", "http://example.com", "x")
            addAttribute("attr2", "http://example2.com", "y")
        }
}
