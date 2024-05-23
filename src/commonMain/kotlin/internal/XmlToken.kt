package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.NO_NAMESPACE_PREFIX

/** Token representing part of an XML document. */
internal sealed interface XmlToken {
    data class ElementStart(val name: String, val prefix: String = NO_NAMESPACE_PREFIX) : XmlToken

    object ElementStartEnd : XmlToken

    data class ElementEnd(val name: String? = null, val prefix: String = NO_NAMESPACE_PREFIX) :
        XmlToken

    data class AttributeStart(val name: String, val prefix: String = NO_NAMESPACE_PREFIX) :
        XmlToken

    data class AttributeValue(val value: String) : XmlToken

    object AttributeEnd : XmlToken

    data class Text(val content: String) : XmlToken

    object DocumentEnd : XmlToken
}
