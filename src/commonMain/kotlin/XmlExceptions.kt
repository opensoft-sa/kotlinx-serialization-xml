package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.SerializationException

/** A generic exception indicating the problem in XML serialization or deserialization process. */
public open class XmlSerializationException(message: String? = null, cause: Throwable? = null) :
    SerializationException(message, cause)

/** Exception that occurs when attempting to use a namespace prefix that hasn't been defined. */
public class UndefinedNamespaceException(prefix: String) :
    XmlSerializationException("Namespace prefix '$prefix' used, but no definition found")
