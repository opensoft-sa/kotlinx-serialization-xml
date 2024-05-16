package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import pt.opensoft.kotlinx.serialization.xml.util.SerializationTest
import pt.opensoft.kotlinx.serialization.xml.util.defaultTestConfig

class MapValuesSerializer<T>(tSerializer: KSerializer<T>) : KSerializer<Map<Int, T>> {
    private val listSerializer = ListSerializer(tSerializer)

    override val descriptor: SerialDescriptor = SerialDescriptor("Map", listSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: Map<Int, T>): Unit =
        encoder.encodeSerializableValue(listSerializer, value.values.toList())

    override fun deserialize(decoder: Decoder): Map<Int, T> =
        decoder.decodeSerializableValue(listSerializer).withIndex().associate { (i, value) ->
            i to value
        }
}

@Serializable
data class ContextualElements(
    @Contextual val intWithoutContext: Int = 42,
    @Contextual val mapAsValuesList: Map<Int, String> = mapOf(0 to "a", 1 to "b")
)

class ContextualClassTest : SerializationTest<ContextualElements>() {
    override val config =
        Xml(defaultTestConfig) {
            serializersModule = SerializersModule {
                contextual(Map::class) { args -> MapValuesSerializer(args[1]) }
            }
        }

    override val serializer = ContextualElements.serializer()
    override val value = ContextualElements()
    override val xml: String =
        """
        <ContextualElements>
            <intWithoutContext>42</intWithoutContext>
            <mapAsValuesList>a</mapAsValuesList>
            <mapAsValuesList>b</mapAsValuesList>
        </ContextualElements>
        """
    override val element: XmlElement =
        buildXmlElement("ContextualElements") {
            addXmlElement("intWithoutContext") { addText("42") }
            addXmlElement("mapAsValuesList") { addText("a") }
            addXmlElement("mapAsValuesList") { addText("b") }
        }
}
