/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith")

package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import pt.opensoft.kotlinx.serialization.xml.internal.writeXml

/**
 * Base class for custom serializers that allows manipulating an abstract XML representation of the
 * class before serialization or deserialization.
 *
 * [XmlTransformingSerializer] provides capabilities to manipulate [XmlElement] representation
 * directly instead of interacting with [Encoder] and [Decoder] in order to apply a custom
 * transformation to the XML. Please note that this class expects that [Encoder] and [Decoder] are
 * implemented by [XmlDecoder] and [XmlEncoder], i.e. serializers derived from this class work only
 * with [Xml] format.
 *
 * There are two methods in which XML transformation can be defined: [transformSerialize] and
 * [transformDeserialize]. You can override one or both of them. Consult their documentation for
 * details.
 *
 * @param T A type for Kotlin property for which this serializer could be applied. **Not** the type
 *   that you may encounter in XML. (e.g. if you unwrap a list to a single value `T`, use `T`, not
 *   `List<T>`)
 * @param tSerializer A serializer for type [T]. Determines [XmlElement] which is passed to
 *   [transformSerialize]. Should be able to parse [XmlElement] from [transformDeserialize]
 *   function. Usually, default [serializer] is sufficient.
 */
public abstract class XmlTransformingSerializer<T : Any>(private val tSerializer: KSerializer<T>) :
    KSerializer<T> {

    /**
     * A descriptor for this transformation. By default, it delegates to [tSerializer]'s descriptor.
     *
     * However, this descriptor can be overridden to achieve better representation of the resulting
     * XML shape for schema generating or introspection purposes.
     */
    override val descriptor: SerialDescriptor
        get() = tSerializer.descriptor

    final override fun serialize(encoder: Encoder, value: T) {
        val output = encoder.asXmlEncoder()
        var element = writeXml(output.xml, value, tSerializer)
        element = transformSerialize(element)
        output.encodeXmlElement(element)
    }

    final override fun deserialize(decoder: Decoder): T {
        val input = decoder.asXmlDecoder()
        val element = input.decodeXmlElement()
        return input.xml.decodeFromXmlElement(tSerializer, transformDeserialize(element))
    }

    /**
     * Transformation that happens during [deserialize] call. Does nothing by default.
     *
     * During deserialization, a value from XML is firstly decoded to an [XmlElement], user
     * transformation in [transformDeserialize] is applied, and then resulting [XmlElement] is
     * deserialized to [T] with [tSerializer].
     */
    protected open fun transformDeserialize(element: XmlElement): XmlElement = element

    /**
     * Transformation that happens during [serialize] call. Does nothing by default.
     *
     * During serialization, a value of type [T] is serialized with [tSerializer] to an
     * [XmlElement], user transformation in [transformSerialize] is applied, and then resulting
     * [XmlElement] is encoded to an XML string.
     */
    protected open fun transformSerialize(element: XmlElement): XmlElement = element
}
