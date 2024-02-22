import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost.DEFAULT
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.mavenPublish)
}

group = "com.ryanharter.kotlinx.serialization"
version = "0.0.1-SNAPSHOT"

repositories {
  mavenCentral()
}

kotlin {
  explicitApi()

  jvm {
    jvmToolchain(8)
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
  }
  js {
    nodejs()
  }

  // Note: Keep native list in sync with kotlinx.serialization:
  // https://github.com/Kotlin/kotlinx.serialization/blob/master/gradle/native-targets.gradle

  // Tier 1
  macosX64()
  macosArm64()
  iosSimulatorArm64()
  iosX64()

  // Tier 2
  linuxX64()
  linuxArm64()
  watchosSimulatorArm64()
  watchosX64()
  watchosArm32()
  watchosArm64()
  tvosSimulatorArm64()
  tvosX64()
  tvosArm64()
  iosArm64()

  // Tier 3
  mingwX64()
  androidNativeArm32()
  androidNativeArm64()
  androidNativeX86()
  androidNativeX64()
  watchosDeviceArm64()
  linuxArm32Hfp()

  sourceSets {
    all {
      languageSettings {
        optIn("kotlinx.serialization.ExperimentalSerializationApi")
      }
    }

    commonMain.dependencies {
      api(libs.kotlinxSerialization.core)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }

  // Add a test binary and execution for native targets which runs on a background thread.
  targets.withType(KotlinNativeTargetWithTests::class).all {
    binaries {
      test("background", listOf(NativeBuildType.DEBUG)) {
        freeCompilerArgs += listOf("-trw")
      }
    }
    testRuns {
      create("background") {
        setExecutionSourceFrom(binaries.getByName("backgroundDebugTest") as TestExecutable)
      }
    }
  }
}

mavenPublishing {
  configure(KotlinMultiplatform())

  publishToMavenCentral(DEFAULT)
  signAllPublications()
  pom {
    description.set("A fully native, multiplatform XML format add-on for Kotlin Serialization.")
    name.set(project.name)
    url.set("https://github.com/rharter/kotlinx-serialization-xml/")
    licenses {
      license {
        name.set("The Apache Software License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("repo")
      }
    }
    scm {
      url.set("https://github.com/rharter/kotlinx-serialization-xml/")
      connection.set("scm:git:git://github.com/rharter/kotlinx-serialization-xml.git")
      developerConnection.set("scm:git:ssh://git@github.com/rharter/kotlinx-serialization-xml.git")
    }
    developers {
      developer {
        id.set("rharter")
        name.set("Ryan Harter")
      }
    }
  }
}
