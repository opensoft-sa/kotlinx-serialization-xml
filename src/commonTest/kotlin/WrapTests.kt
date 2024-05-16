package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest

@Serializable data class Wrapper(@XmlAttribute val value: String)

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

class WrapTest : SerializationTest<ElementWrapping>() {
    override val serializer = ElementWrapping.serializer()
    override val value = ElementWrapping()
    override val xml: String =
        """
        <ElementWrapping>
            <flatString>x</flatString>
            <wrapString>
                <String>x</String>
            </wrapString>
            <flatClass value="x" />
            <wrapClass>
                <Wrapper value="x" />
            </wrapClass>
            <flatStringList>x</flatStringList>
            <flatStringList>y</flatStringList>
            <wrapStringList>
                <String>x</String>
                <String>y</String>
            </wrapStringList>
            <flatClassList value="x" />
            <flatClassList value="y" />
            <wrapClassList>
                <Wrapper value="x" />
                <Wrapper value="y" />
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
                <Wrapper value="x" />
                <Wrapper value="y" />
            </flatClassListList>
            <flatClassListList>
                <Wrapper value="w" />
                <Wrapper value="z" />
            </flatClassListList>
            <wrapClassListList>
                <ArrayList>
                    <Wrapper value="x" />
                    <Wrapper value="y" />
                </ArrayList>
                <ArrayList>
                    <Wrapper value="w" />
                    <Wrapper value="z" />
                </ArrayList>
            </wrapClassListList>
        </ElementWrapping>
        """
    override val element: XmlElement =
        buildXmlElement("ElementWrapping") {
            addXmlElement("flatString") { addText("x") }
            addXmlElement("wrapString") { addXmlElement("String") { addText("x") } }
            addXmlElement("flatClass") { addAttribute("value", "x") }
            addXmlElement("wrapClass") { addXmlElement("Wrapper") { addAttribute("value", "x") } }
            addXmlElement("flatStringList") { addText("x") }
            addXmlElement("flatStringList") { addText("y") }
            addXmlElement("wrapStringList") {
                addXmlElement("String") { addText("x") }
                addXmlElement("String") { addText("y") }
            }
            addXmlElement("flatClassList") { addAttribute("value", "x") }
            addXmlElement("flatClassList") { addAttribute("value", "y") }
            addXmlElement("wrapClassList") {
                addXmlElement("Wrapper") { addAttribute("value", "x") }
                addXmlElement("Wrapper") { addAttribute("value", "y") }
            }
            addXmlElement("flatStringListList") {
                addXmlElement("String") { addText("x") }
                addXmlElement("String") { addText("y") }
            }
            addXmlElement("flatStringListList") {
                addXmlElement("String") { addText("w") }
                addXmlElement("String") { addText("z") }
            }
            addXmlElement("wrapStringListList") {
                addXmlElement("ArrayList") {
                    addXmlElement("String") { addText("x") }
                    addXmlElement("String") { addText("y") }
                }
                addXmlElement("ArrayList") {
                    addXmlElement("String") { addText("w") }
                    addXmlElement("String") { addText("z") }
                }
            }
            addXmlElement("flatClassListList") {
                addXmlElement("Wrapper") { addAttribute("value", "x") }
                addXmlElement("Wrapper") { addAttribute("value", "y") }
            }
            addXmlElement("flatClassListList") {
                addXmlElement("Wrapper") { addAttribute("value", "w") }
                addXmlElement("Wrapper") { addAttribute("value", "z") }
            }
            addXmlElement("wrapClassListList") {
                addXmlElement("ArrayList") {
                    addXmlElement("Wrapper") { addAttribute("value", "x") }
                    addXmlElement("Wrapper") { addAttribute("value", "y") }
                }
                addXmlElement("ArrayList") {
                    addXmlElement("Wrapper") { addAttribute("value", "w") }
                    addXmlElement("Wrapper") { addAttribute("value", "z") }
                }
            }
        }
}

@Serializable
data class NamedElementWrapping(
    @XmlWrap @XmlWrappedName("wrappedString") val wrapString: String = "x",
    @XmlWrap @XmlWrappedName("wrappedClass") val wrapClass: Wrapper = Wrapper("x"),
    @XmlWrap
    @XmlWrappedName("wrappedStringItem")
    val wrapStringList: List<String> = listOf("x", "y"),
    @XmlWrap
    @XmlWrappedName("wrappedClassItem")
    val wrapClassList: List<Wrapper> = listOf(Wrapper("x"), Wrapper("y")),
    @XmlWrap
    @XmlWrappedName("wrappedStringListItem")
    val wrapStringListList: List<List<String>> = listOf(listOf("x", "y"), listOf("w", "z")),
    @XmlWrap
    @XmlWrappedName("wrappedClassListItem")
    val wrapClassListList: List<List<Wrapper>> =
        listOf(listOf(Wrapper("x"), Wrapper("y")), listOf(Wrapper("w"), Wrapper("z")))
)

class NamedWrapTest : SerializationTest<NamedElementWrapping>() {
    override val serializer = NamedElementWrapping.serializer()
    override val value = NamedElementWrapping()
    override val xml: String =
        """
        <NamedElementWrapping>
            <wrapString>
                <wrappedString>x</wrappedString>
            </wrapString>
            <wrapClass>
                <wrappedClass value="x" />
            </wrapClass>
            <wrapStringList>
                <wrappedStringItem>x</wrappedStringItem>
                <wrappedStringItem>y</wrappedStringItem>
            </wrapStringList>
            <wrapClassList>
                <wrappedClassItem value="x" />
                <wrappedClassItem value="y" />
            </wrapClassList>
            <wrapStringListList>
                <wrappedStringListItem>
                    <String>x</String>
                    <String>y</String>
                </wrappedStringListItem>
                <wrappedStringListItem>
                    <String>w</String>
                    <String>z</String>
                </wrappedStringListItem>
            </wrapStringListList>
            <wrapClassListList>
                <wrappedClassListItem>
                    <Wrapper value="x" />
                    <Wrapper value="y" />
                </wrappedClassListItem>
                <wrappedClassListItem>
                    <Wrapper value="w" />
                    <Wrapper value="z" />
                </wrappedClassListItem>
            </wrapClassListList>
        </NamedElementWrapping>
        """
    override val element: XmlElement =
        buildXmlElement("NamedElementWrapping") {
            addXmlElement("wrapString") { addXmlElement("wrappedString") { addText("x") } }
            addXmlElement("wrapClass") {
                addXmlElement("wrappedClass") { addAttribute("value", "x") }
            }
            addXmlElement("wrapStringList") {
                addXmlElement("wrappedStringItem") { addText("x") }
                addXmlElement("wrappedStringItem") { addText("y") }
            }
            addXmlElement("wrapClassList") {
                addXmlElement("wrappedClassItem") { addAttribute("value", "x") }
                addXmlElement("wrappedClassItem") { addAttribute("value", "y") }
            }
            addXmlElement("wrapStringListList") {
                addXmlElement("wrappedStringListItem") {
                    addXmlElement("String") { addText("x") }
                    addXmlElement("String") { addText("y") }
                }
                addXmlElement("wrappedStringListItem") {
                    addXmlElement("String") { addText("w") }
                    addXmlElement("String") { addText("z") }
                }
            }
            addXmlElement("wrapClassListList") {
                addXmlElement("wrappedClassListItem") {
                    addXmlElement("Wrapper") { addAttribute("value", "x") }
                    addXmlElement("Wrapper") { addAttribute("value", "y") }
                }
                addXmlElement("wrappedClassListItem") {
                    addXmlElement("Wrapper") { addAttribute("value", "w") }
                    addXmlElement("Wrapper") { addAttribute("value", "z") }
                }
            }
        }
}

@Serializable
data class NamespacedElementWrapping(
    @XmlWrap
    @XmlWrappedName("wrappedString")
    @XmlWrappedNamespace("http://example.com", "ex")
    val wrapString: String = "x",
    @XmlWrap
    @XmlWrappedName("wrappedClass")
    @XmlWrappedNamespace("http://example.com", "ex")
    val wrapClass: Wrapper = Wrapper("x"),
    @XmlWrap
    @XmlWrappedName("wrappedStringItem")
    @XmlWrappedNamespace("http://example.com", "ex")
    val wrapStringList: List<String> = listOf("x", "y"),
    @XmlWrap
    @XmlWrappedName("wrappedClassItem")
    @XmlWrappedNamespace("http://example.com", "ex")
    val wrapClassList: List<Wrapper> = listOf(Wrapper("x"), Wrapper("y")),
    @XmlWrap
    @XmlWrappedName("wrappedStringListItem")
    @XmlWrappedNamespace("http://example.com", "ex")
    val wrapStringListList: List<List<String>> = listOf(listOf("x", "y"), listOf("w", "z")),
    @XmlWrap
    @XmlWrappedName("wrappedClassListItem")
    @XmlWrappedNamespace("http://example.com", "ex")
    val wrapClassListList: List<List<Wrapper>> =
        listOf(listOf(Wrapper("x"), Wrapper("y")), listOf(Wrapper("w"), Wrapper("z")))
)

class NamespacedWrapTest : SerializationTest<NamespacedElementWrapping>() {
    override val serializer = NamespacedElementWrapping.serializer()
    override val value = NamespacedElementWrapping()
    override val xml: String =
        """
        <NamespacedElementWrapping xmlns:ex="http://example.com">
            <wrapString>
                <ex:wrappedString>x</ex:wrappedString>
            </wrapString>
            <wrapClass>
                <ex:wrappedClass value="x" />
            </wrapClass>
            <wrapStringList>
                <ex:wrappedStringItem>x</ex:wrappedStringItem>
                <ex:wrappedStringItem>y</ex:wrappedStringItem>
            </wrapStringList>
            <wrapClassList>
                <ex:wrappedClassItem value="x" />
                <ex:wrappedClassItem value="y" />
            </wrapClassList>
            <wrapStringListList>
                <ex:wrappedStringListItem>
                    <String>x</String>
                    <String>y</String>
                </ex:wrappedStringListItem>
                <ex:wrappedStringListItem>
                    <String>w</String>
                    <String>z</String>
                </ex:wrappedStringListItem>
            </wrapStringListList>
            <wrapClassListList>
                <ex:wrappedClassListItem>
                    <Wrapper value="x" />
                    <Wrapper value="y" />
                </ex:wrappedClassListItem>
                <ex:wrappedClassListItem>
                    <Wrapper value="w" />
                    <Wrapper value="z" />
                </ex:wrappedClassListItem>
            </wrapClassListList>
        </NamespacedElementWrapping>
        """
    override val element: XmlElement =
        buildXmlElement("NamespacedElementWrapping") {
            declareNamespace("http://example.com", "ex")
            addXmlElement("wrapString") {
                addXmlElement("wrappedString", "http://example.com") { addText("x") }
            }
            addXmlElement("wrapClass") {
                addXmlElement("wrappedClass", "http://example.com") { addAttribute("value", "x") }
            }
            addXmlElement("wrapStringList") {
                addXmlElement("wrappedStringItem", "http://example.com") { addText("x") }
                addXmlElement("wrappedStringItem", "http://example.com") { addText("y") }
            }
            addXmlElement("wrapClassList") {
                addXmlElement("wrappedClassItem", "http://example.com") {
                    addAttribute("value", "x")
                }
                addXmlElement("wrappedClassItem", "http://example.com") {
                    addAttribute("value", "y")
                }
            }
            addXmlElement("wrapStringListList") {
                addXmlElement("wrappedStringListItem", "http://example.com") {
                    addXmlElement("String") { addText("x") }
                    addXmlElement("String") { addText("y") }
                }
                addXmlElement("wrappedStringListItem", "http://example.com") {
                    addXmlElement("String") { addText("w") }
                    addXmlElement("String") { addText("z") }
                }
            }
            addXmlElement("wrapClassListList") {
                addXmlElement("wrappedClassListItem", "http://example.com") {
                    addXmlElement("Wrapper") { addAttribute("value", "x") }
                    addXmlElement("Wrapper") { addAttribute("value", "y") }
                }
                addXmlElement("wrappedClassListItem", "http://example.com") {
                    addXmlElement("Wrapper") { addAttribute("value", "w") }
                    addXmlElement("Wrapper") { addAttribute("value", "z") }
                }
            }
        }
}
