package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.XmlElement

/** Function used to decode an XML element. */
internal fun XmlLexer.decodeXmlElement(namespaces: Map<String, String>): XmlElement {
    val elementNamespaces = namespaces.toMutableMap()
    return XmlElement("TODO")
}
