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
    jvm {   }
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

                implementation("org.hamcrest:hamcrest-all:_")

                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:_")
                implementation("com.fasterxml.jackson.core:jackson-annotations:_")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:_")

                // kotlintest runner needs this to enable logging
                implementation("org.slf4j:slf4j-api:_")
                implementation("org.slf4j:jcl-over-slf4j:_")
                implementation("org.slf4j:log4j-over-slf4j:_")
                implementation("org.slf4j:jul-to-slf4j:_")
                implementation("ch.qos.logback:logback-classic:_")
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
