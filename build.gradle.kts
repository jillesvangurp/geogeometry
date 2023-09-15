plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        val main by compilations.getting {
            kotlinOptions {
                // Setup the Kotlin compiler options for the 'main' compilation:
                jvmTarget = "1.8"
            }
        }
        val test by compilations.getting {
            kotlinOptions {
                // Setup the Kotlin compiler options for the 'main' compilation:
                jvmTarget = "1.8"
            }
        }
    }
    js(IR) {
        nodejs {
            testTask(Action {
                useMocha {
                    // javascript is a lot slower than Java, we hit the default timeout of 2000
                    timeout = "20000"
                }
            })
        }
    }

    sourceSets {

        val commonMain by getting {
                dependencies {
                    implementation(kotlin("stdlib-common"))
                    api("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
                }
            }

        val commonTest by getting {
                dependencies {
                    implementation(kotlin("test-common"))
                    implementation(kotlin("test-annotations-common"))
                    // yay kotest does multiplatform
                    implementation("io.kotest:kotest-assertions-core:_")

                    api("org.jetbrains.kotlinx:kotlinx-serialization-cbor:_")
                }
            }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
            }
        }
        val jvmTest by getting {
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

        val jsMain by getting {
                dependencies {
                    implementation(kotlin("stdlib-js"))
                    api("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
                }
        }

        val jsTest by getting {
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
            url = uri("file://$projectDir/localRepo")
        }
    }
}
