# Kotlin serialization XML

A fully native, multiplatform XML format add-on
for [Kotlin serialization](https://github.com/Kotlin/kotlinx.serialization).

Kotlin serialization XML provides an `Xml` format for Kotlin serialization, allowing you to use the
standard `@Serializable` annotation to create reflectionless, multiplatform serializers for your
Kotlin classes.

- Supports serialization of class properties as either attributes or elements.
- XML namespace aware.

## Introduction

Here is a small example:

```kotlin
import kotlinx.serialization.*
import pt.opensoft.kotlinx.serialization.xml.*

@Serializable
data class Greeting(
    @XmlAttribute val from: String,
    @XmlAttribute val to: String,
    val message: Message
)

@Serializable
data class Message(@XmlText val content: String)

fun main() {
    // Serializing objects
    val data = Greeting("Ryan", "Bill", Message("Hi"))
    val string = Xml.encodeToString(data)
    println(string) // <Greeting from="Ryan" to="Bill"><message>Hi</message></Greeting>
    // Deserializing back into objects
    val obj = Xml.decodeFromString<Greeting>(string)
    println(obj) // Greeting(from=Ryan, to=Bill, message=Message(content=Hi))
}

```

## Installation

```kotlin
dependencies {
    implementation("pt.opensoft:kotlinx-serialization-xml:0.0.1-SNAPSHOT")
}
```

# License

```
Copyright 2024 Opensoft

Copyright 2022 Ryan Harter

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
