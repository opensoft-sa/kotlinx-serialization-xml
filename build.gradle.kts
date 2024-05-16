import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.nexusPublish)
    `maven-publish`
    signing
}

group = "pt.opensoft"

version = "0.0.1-SNAPSHOT"

repositories { mavenCentral() }

kotlin {
    explicitApi()

    jvmToolchain(8)
    jvm { testRuns["test"].executionTask.configure { useJUnitPlatform() } }
    js { nodejs() }

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
        all { languageSettings { optIn("kotlinx.serialization.ExperimentalSerializationApi") } }

        commonMain.dependencies { api(libs.kotlinxSerialization.core) }
        commonTest.dependencies { implementation(kotlin("test")) }
    }

    // Add a test binary and execution for native targets which runs on a background thread.
    targets.withType(KotlinNativeTargetWithTests::class).all {
        binaries {
            test("background", listOf(NativeBuildType.DEBUG)) { freeCompilerArgs += listOf("-trw") }
        }
        testRuns {
            create("background") {
                setExecutionSourceFrom(binaries.getByName("backgroundDebugTest") as TestExecutable)
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl = uri("https://s01.oss.sonatype.org/service/local/")
            snapshotRepositoryUrl =
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}

val javadocJar by tasks.registering(Jar::class) { archiveClassifier = "javadoc" }

publishing {
    publications.withType<MavenPublication> {
        artifact(javadocJar)

        pom {
            name = project.name
            description =
                "A fully native, multiplatform XML format add-on for Kotlin serialization."
            url = "https://github.com/opensoft-sa/kotlinx-serialization-xml/"
            licenses {
                license {
                    name = "The Apache Software License, Version 2.0"
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    distribution = "repo"
                }
            }
            developers {
                developer {
                    id = "opensoft-sa"
                    name = "Opensoft"
                }
                developer {
                    id = "rharter"
                    name = "Ryan Harter"
                }
            }
            scm { url = "https://github.com/opensoft-sa/kotlinx-serialization-xml/" }
        }
    }
}

signing {
    setRequired { !"$version".endsWith("-SNAPSHOT") && gradle.taskGraph.hasTask("publish") }
    sign(publishing.publications)
}
