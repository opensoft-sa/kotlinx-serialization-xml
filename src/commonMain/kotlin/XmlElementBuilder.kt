@file:OptIn(ExperimentalContracts::class)

package pt.opensoft.kotlinx.serialization.xml

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Builds [XmlElement] with the given [builderAction] builder. Example of usage:
 * ```
 * val xml = buildXmlElement("rootElement") {
 *     addAttribute("elementAttribute", "Attribute value")
 *     addXmlElement("innerElement") {
 *         addText("Inner element text")
 *     }
 * }
 * ```
 */
public inline fun buildXmlElement(
    name: String,
    namespace: String = NO_NAMESPACE_URI,
    builderAction: XmlElementBuilder.() -> Unit = {}
): XmlElement {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    val builder = XmlElementBuilder(name, namespace)
    builder.builderAction()
    return builder.build()
}

/**
 * DSL builder for an [XmlElement]. To create an instance of builder, use [buildXmlElement] build
 * function.
 */
public class XmlElementBuilder
@PublishedApi
internal constructor(private val name: String, private val namespace: String = NO_NAMESPACE_URI) {
    private val attributes: MutableSet<XmlElement.Attribute> = mutableSetOf()
    private val content: MutableList<XmlContent> = mutableListOf()

    /**
     * Add the given XML [attribute] to the attributes of a resulting XML element.
     *
     * @return `true` if the attribute has been added, `false` if the attribute is already contained
     *   in the attributes of the XML element.
     */
    public fun addAttribute(attribute: XmlElement.Attribute): Boolean = attributes.add(attribute)

    /**
     * Add all given XML [attributes] to the attributes of a resulting XML element.
     *
     * @return `true` if any of the specified attributes was added, `false` if the attributes of the
     *   XML element were not modified.
     */
    public fun addAllAttributes(attributes: Collection<XmlElement.Attribute>): Boolean =
        this.attributes.addAll(attributes)

    /**
     * Add the given XML [content] to the content of a resulting XML element.
     *
     * @return Always `true`, similarly to [ArrayList] specification.
     */
    public fun addContent(content: XmlContent): Boolean = this.content.add(content)

    /**
     * Add all given XML [content] to the content of a resulting XML element.
     *
     * @return `true` if content of the XML element was changed as the result of the operation.
     */
    public fun addAllContent(content: Collection<XmlContent>): Boolean =
        this.content.addAll(content)

    @PublishedApi
    internal fun build(): XmlElement = XmlElement(name, namespace, attributes, content)
}

/**
 * Add an XML attribute with the provided [name], [namespace], and [value] to the attributes of a
 * resulting XML element.
 *
 * @return `true` if the attribute has been added, `false` if the attribute is already contained in
 *   the attributes of the XML element.
 */
public fun XmlElementBuilder.addAttribute(name: String, namespace: String, value: String): Boolean =
    addAttribute(XmlElement.Attribute(name, namespace, value))

/**
 * Add an XML attribute with the provided [name] and [value] to the attributes of a resulting XML
 * element.
 *
 * @return `true` if the attribute has been added, `false` if the attribute is already contained in
 *   the attributes of the XML element.
 */
public fun XmlElementBuilder.addAttribute(name: String, value: String): Boolean =
    addAttribute(name, NO_NAMESPACE_URI, value)

/**
 * Add an XML attribute declaring a default namespace with the provided [uri] to the attributes of a
 * resulting XML element.
 *
 * @return `true` if the attribute has been added, `false` if the attribute is already contained in
 *   the attributes of the XML element.
 */
public fun XmlElementBuilder.declareNamespace(uri: String): Boolean =
    addAttribute(XMLNS_NAMESPACE_PREFIX, uri)

/**
 * Add an XML attribute declaring a namespace with the provided [uri] and [prefix] to the attributes
 * of a resulting XML element.
 *
 * @return `true` if the attribute has been added, `false` if the attribute is already contained in
 *   the attributes of the XML element.
 */
public fun XmlElementBuilder.declareNamespace(uri: String, prefix: String): Boolean =
    addAttribute(prefix, XMLNS_NAMESPACE_URI, uri)

/**
 * Add an XML element with the provided [name] and [namespace] produced by the [builderAction]
 * function to the content of a resulting XML element.
 *
 * @return Always `true`, similarly to [ArrayList] specification.
 */
public fun XmlElementBuilder.addXmlElement(
    name: String,
    namespace: String = NO_NAMESPACE_URI,
    builderAction: XmlElementBuilder.() -> Unit = {}
): Boolean = addContent(buildXmlElement(name, namespace, builderAction))

/**
 * Add an XML text with the provided [content] to the content of a resulting XML element.
 *
 * @return Always `true`, similarly to [ArrayList] specification.
 */
public fun XmlElementBuilder.addText(content: String): Boolean =
    addContent(XmlElement.Text(content))

/**
 * Add an XML text with the provided [content] (transformed into a string via [toString]) to the
 * content of a resulting XML element.
 *
 * @return Always `true`, similarly to [ArrayList] specification.
 */
public fun XmlElementBuilder.addText(content: Any): Boolean = addText(content.toString())
