package pt.opensoft.kotlinx.serialization.xml

import kotlinx.serialization.Serializable

enum class GreetingType {
    Formal,
    Informal,
    Unspecified
}

@Serializable data class Greetings(val greetings: List<Greeting> = emptyList())

@XmlName("Greeting")
@Serializable
data class Greeting(
    @XmlAttribute val from: String,
    @XmlAttribute val to: String,
    val message: Message,
    val type: GreetingType = GreetingType.Unspecified,
    val ps: String? = null
)

@Serializable data class Message(@XmlText val content: String)
