package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.SerialInfo

/**
 * Use [DeclaresXmlNamespace] to explicitly declare a namespace within an element. Declared
 * namespaces will be in scope for the current element, as well as all of its descendants. This
 * annotation is especially useful to specify the default namespace (via
 * `@XmlNamespaceDeclaration("defaultNamespaceUri")`, typically in the root element.
 *
 * Namespaces can also be implicitly declared when specifying an element or attribute's namespace
 * via [XmlNamespace]. However, these implicit namespace declarations will be declared in the
 * element that contains such annotations, which might not be the root element, thus generating
 * "noisier" output.
 *
 * Multiple namespaces may be declared by repeating the [DeclaresXmlNamespace] annotation.
 *
 * This annotation cannot be used together with [XmlAttribute] or [XmlText].
 */
@SerialInfo
@Repeatable
@Target(AnnotationTarget.CLASS)
public annotation class DeclaresXmlNamespace(
    /** Namespace name, identified by a URI. */
    public val uri: String,
    /** Namespace prefix. If left empty, the default namespace will be declared. */
    public val prefix: String = ""
)

/**
 * Annotation used to specify the name of an XML element or property. This annotation serves a
 * similar purpose to the [SerialName][kotlinx.serialization.SerialName] annotation, but
 * specifically for the XML format and, as such, has precedence over
 * [SerialName][kotlinx.serialization.SerialName] when both are specified.
 *
 * [XmlName] can be used to specify multiple properties of the same class with the same name but
 * different namespaces (which can be specified via [XmlNamespace]).
 *
 * Cannot be used together with [XmlText].
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlName(public val value: String)

/**
 * Annotation used to specify the name of an XML element representing the item of a list or map.
 *
 * Use together with [XmlItemNamespace] to fully qualify the item's name.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlItemName(public val value: String)

/**
 * Annotation used to specify the namespace of an element or attribute.
 *
 * If no [XmlNamespace] is provided to a class or property representing an XML element, then the
 * element's namespace is implicitly the default namespace in scope (if such a namespace exists)
 * rather than the element having no namespace. To explicitly specify that an element has no
 * namespace, use `@XmlNamespace("")`.
 *
 * Attributes (properties annotated with [XmlAttribute]) with no [XmlNamespace] implicitly have no
 * namespace.
 *
 * If the provided namespace is not in scope, then a namespace declaration will be automatically
 * created in the containing element, with a prefix based on [preferredPrefix] when one is provided,
 * or an automatically generated one when not. If the provided namespace is already in scope, then
 * [preferredPrefix] will be ignored and the prefix of the namespace in scope will be used.
 *
 * This annotation cannot be used together with [XmlText].
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlNamespace(
    /** Namespace name, identified by a URI. */
    public val uri: String,
    /**
     * Optional preferred namespace prefix used when auto-generating a namespace declaration. This
     * value will be ignored when the namespace is already in scope.
     */
    public val preferredPrefix: String = ""
)

/**
 * Annotation used to specify the namespace of an XML element representing the item of a list or
 * map.
 *
 * Use together with [XmlItemName] to fully qualify the item's name.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlItemNamespace(
    /** Namespace name, identified by a URI. */
    public val uri: String,
    /**
     * Optional preferred namespace prefix used when auto-generating a namespace declaration. This
     * value will be ignored when the namespace is already in scope.
     */
    public val preferredPrefix: String = ""
)

/** Properties annotated with [XmlAttribute] will be serialized as attributes of an XML element. */
@SerialInfo @Target(AnnotationTarget.PROPERTY) public annotation class XmlAttribute

/**
 * A property annotated with [XmlText] will be serialized as the text content of an element.
 *
 * There can only exist a single property annotated with [XmlText] per class.
 */
@SerialInfo @Target(AnnotationTarget.PROPERTY) public annotation class XmlText

/**
 * Annotation used to specify that a structured property should be wrapped in an element, when
 * [wrapStructuredProperties][XmlBuilder.wrapStructuredProperties] is set to `false` (the default).
 */
@SerialInfo @Target(AnnotationTarget.PROPERTY) public annotation class XmlWrap
