@file:OptIn(ExperimentalWasmDsl::class)

import java.time.Duration
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        content {
            includeGroup("com.jillesvangurp")
        }
    }
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    js(IR) {
        nodejs {
            testTask {
                useMocha {
                    // javascript is a lot slower than Java, we hit the default timeout of 2000
                    timeout = "60s"
                }
            }
        }
    }
    linuxX64()
    linuxArm64()
    mingwX64()
    macosX64()
    macosArm64()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    wasmJs {
        browser {
            testTask {
                // FIXME ignored
                timeout= Duration.ofSeconds(60)
            }
        }
        nodejs {
            testTask {
                timeout= Duration.ofSeconds(60)
            }
        }
        d8 {
            testTask {
                timeout= Duration.ofSeconds(60)
            }
        }
    }
    // no kotest support yet for this
//    wasmWasi()

    sourceSets {

        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
                implementation("com.jillesvangurp:kotlinx-serialization-extensions:_")

            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                // yay kotest does multiplatform
                implementation("io.kotest:kotest-assertions-core:_")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:_")
            }
        }

        jvmMain {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
            }
        }
        jvmTest {
            dependencies {
                runtimeOnly("org.junit.jupiter:junit-jupiter:_")
                implementation(kotlin("test-junit"))
            }
        }

        jsMain {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
            }
        }

        jsTest {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        wasmJsTest {
            dependencies {
                implementation(kotlin("test-wasm-js"))
            }
        }

        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
//                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                languageVersion = "2.0"
                apiVersion = "2.0"
            }
        }
    }
}

tasks.named("iosSimulatorArm64Test") {
    // requires IOS simulator and tens of GB of other stuff to be installed
    // so keep it disabled
    enabled = false
}

listOf("iosSimulatorArm64Test","wasmJsTest","wasmJsBrowserTest","wasmJsNodeTest","wasmJsD8Test").forEach {target->
    // skip the test weirdness for now
    tasks.named(target) {
        // requires IOS simulator and tens of GB of other stuff to be installed
        // so keep it disabled
        enabled = false
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("GeoGeometry")
                description.set("A Kotlin Multiplatform library for geospatial geometry operations.")
                url.set("https://github.com/jillesvangurp/geogeometry")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/jillesvangurp/geogeometry/blob/master/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("jillesvangurp")
                        name.set("Jilles van Gurp")
                        email.set("jilles@no-reply.github.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/jillesvangurp/geogeometry.git")
                    developerConnection.set("scm:git:ssh://github.com:jillesvangurp/geogeometry.git")
                    url.set("https://github.com/jillesvangurp/geogeometry")
                }
            }
        }
    }
    repositories {
        maven {
            // GOOGLE_APPLICATION_CREDENTIALS env var must be set for this to work
            // public repository is at https://maven.tryformation.com/releases
            url = uri("gcs://mvn-public-tryformation/releases")
            name = "FormationPublic"
        }
    }
}
