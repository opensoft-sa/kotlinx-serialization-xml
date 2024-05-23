package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import pt.opensoft.kotlinx.serialization.xml.internal.getXmlName

/** A generic exception indicating a problem in XML serialization or deserialization. */
public open class XmlException internal constructor(message: String? = null) :
    SerializationException(message)

/**
 * Exception thrown when [Xml] has failed to parse the given XML string or deserialize it into a
 * target class.
 */
public open class XmlDecodingException internal constructor(message: String) :
    XmlException(message)

/**
 * Exception thrown when [Xml] has found an unexpected token while parsing an XML string.
 *
 * @property offset Offset at which the unexpected token was found.
 */
public class UnexpectedXmlTokenException
internal constructor(public val offset: Int, message: String) :
    XmlDecodingException("Unexpected XML token at offset $offset: $message")

/**
 * Exception that occurs when attempting to use a namespace prefix that hasn't been declared.
 *
 * @property offset Offset at which the unknown namespace prefix was found.
 * @property prefix Undeclared namespace prefix.
 */
public class UndeclaredNamespacePrefixException
internal constructor(public val offset: Int, public val prefix: String) :
    XmlDecodingException("Undeclared namespace prefix at offset $offset: '$prefix'")

/** Exception thrown when [Xml] has failed to create an XML string from the given value. */
public class XmlEncodingException internal constructor(message: String) : XmlException(message)

/** Exception related to an error in an XML descriptor. */
public open class XmlDescriptorException
internal constructor(descriptor: SerialDescriptor, message: String) :
    XmlException("At '${descriptor.getXmlName()}': $message")
