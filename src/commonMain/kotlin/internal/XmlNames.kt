package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.NO_NAMESPACE_PREFIX
import pt.opensoft.kotlinx.serialization.xml.NO_NAMESPACE_URI

/** Representation of a qualified XML name, as a name together with its namespace URI. */
internal data class QualifiedName(val name: String, val namespace: String = NO_NAMESPACE_URI)

/** Representation of a prefixed XML name, as a name together with its prefix. */
internal data class PrefixedName(val name: String, val prefix: String = NO_NAMESPACE_PREFIX)
