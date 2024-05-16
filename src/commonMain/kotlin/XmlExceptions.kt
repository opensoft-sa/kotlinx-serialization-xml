package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import pt.opensoft.kotlinx.serialization.xml.internal.getXmlName

/** A generic exception indicating a problem in XML serialization and deserialization. */
public open class XmlException(message: String? = null, cause: Throwable? = null) :
    SerializationException(message, cause)

/** Exception related to an XML descriptor */
public open class XmlDescriptorException(descriptor: SerialDescriptor, message: String) :
    XmlException("At '${descriptor.getXmlName()}': $message")

/** Exception that occurs when attempting to use a namespace prefix that hasn't been defined. */
public class UndefinedNamespaceException(prefix: String) :
    XmlException("Namespace prefix '$prefix' used, but no definition found.")
