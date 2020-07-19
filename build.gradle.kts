import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("multiplatform") version "1.3.72"
    id("com.github.ben-manes.versions") version "0.28.0" // gradle dependencyUpdates -Drevision=release
    id("org.jmailen.kotlinter") version "2.4.1"
    `maven-publish`
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}
group = "com.github.jillesvangurp"
version = "0.1-SNAPSHOT"

val kotlinVersion = "1.3.72"
val slf4jVersion = "1.7.26"
val junitVersion = "5.6.2"

kotlin {

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }

        }

        js() {
            // this.mavenPublication {
            //     groupId = artifactGroup
            //     artifactId = project.name
            // }
            val main by compilations.getting {
                dependencies {
                    implementation(kotlin("stdlib-js"))
                }
            }
        }
        // JVM-specific tests and their dependencies:
        jvm() {
            // this.mavenPublication {
            //     groupId = artifactGroup
            //     artifactId = project.name
            // }
            val main by compilations.getting {
                this.kotlinOptions {
                    jvmTarget = "1.8"
                }
                dependencies {
                    implementation(kotlin("stdlib-jdk8"))
                }
            }
            val test by compilations.getting {
                this.kotlinOptions {
                    jvmTarget = "1.8"
                }
                dependencies {
                    implementation("org.junit.jupiter:junit-jupiter:$junitVersion")
                    implementation("io.kotest:kotest-assertions-core-jvm:4.1.1")
                    implementation("org.hamcrest:hamcrest-all:1.3")

                    // kotlintest runner needs this to enable logging
                    implementation("org.slf4j:slf4j-api:$slf4jVersion")
                    implementation("org.slf4j:jcl-over-slf4j:$slf4jVersion")
                    implementation("org.slf4j:log4j-over-slf4j:$slf4jVersion")
                    implementation("org.slf4j:jul-to-slf4j:$slf4jVersion")
                    implementation("ch.qos.logback:logback-classic:1.2.3")

                    implementation("com.google.code.gson:gson:2.8.6")
                }
            }
        }
    }
}

val artifactName = "geogeometry"
val artifactGroup = "com.github.jillesvangurp"

publishing {
    repositories {
        maven {
            name="localrepo"
            url = uri("file://$buildDir/$name")
        }
    }
    // publications {
    //
    //     create<MavenPublication>("lib") {
    //         groupId = artifactGroup
    //         artifactId = artifactName
    //         from(components["jvmMain"])
    //     }
    // }
}

kotlinter {
    ignoreFailures = true
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging.exceptionFormat = TestExceptionFormat.FULL
    testLogging.events = setOf(
        TestLogEvent.FAILED,
        TestLogEvent.PASSED,
        TestLogEvent.SKIPPED,
        TestLogEvent.STANDARD_ERROR,
        TestLogEvent.STANDARD_OUT
    )
}
