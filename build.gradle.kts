import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
//    id("com.android.library") version "3.6.1"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {

    }
    js(IR) {
        nodejs {
            testTask(Action {
                useMocha {
                    // javascript is a lot slower than Java, we hit the default timeout of 2000
                    timeout = "60s"
                }
            })
        }
    }
    linuxX64()
    linuxArm64()
    mingwX64()
    macosX64()
    macosArm64()
//    androidTarget {
//        publishLibraryVariants("release", "debug")
//    }
    iosArm64()
    iosX64()
    // no kotlinx serialization for wasm yet
//    wasmJs()

    sourceSets {

        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                // yay kotest does multiplatform
                implementation("io.kotest:kotest-assertions-core:_")

                api("org.jetbrains.kotlinx:kotlinx-serialization-cbor:_")
            }
        }

        jvmMain  {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
            }
        }
        jvmTest {
            dependencies {
                runtimeOnly("org.junit.jupiter:junit-jupiter:_")
                implementation(kotlin("test-junit"))
            }
        }

        jsMain  {
            dependencies {
                implementation(kotlin("stdlib-js"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
            }
        }

        jsTest  {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

tasks.withType<KotlinJvmCompile> {
    jvmTargetValidationMode.set(JvmTargetValidationMode.WARNING)

    kotlinOptions {
        // this is the minimum LTS version we support, 8 is no longer supported
        jvmTarget = "11"
        languageVersion = "1.9"
    }
}

publishing {
    repositories {
        maven {
            // GOOGLE_APPLICATION_CREDENTIALS env var must be set for this to work
            // public repository is at https://maven.tryformation.com/releases
            url = uri("gcs://mvn-public-tryformation/releases")
            name = "FormationPublic"
        }
    }
}
