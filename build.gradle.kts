import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.ben-manes.versions") version "0.28.0" // gradle dependencyUpdates -Drevision=release
    id("org.jmailen.kotlinter") version "2.4.1"
    `maven-publish`
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

val kotlinVersion = "1.3.72"
val slf4jVersion = "1.7.26"
val junitVersion = "5.6.2"

dependencies {
    // our only run time dependency is the kotlin-stdlib, which works on any platform
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.1.1")
    testImplementation("org.hamcrest:hamcrest-all:1.3")

    // kotlintest runner needs this to enable logging
    testImplementation("org.slf4j:slf4j-api:$slf4jVersion")
    testImplementation("org.slf4j:jcl-over-slf4j:$slf4jVersion")
    testImplementation("org.slf4j:log4j-over-slf4j:$slf4jVersion")
    testImplementation("org.slf4j:jul-to-slf4j:$slf4jVersion")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("com.google.code.gson:gson:2.8.6")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

val artifactName = "geogeometry"
val artifactGroup = "com.github.jillesvangurp"

publishing {
    publications {
        create<MavenPublication>("lib") {
            groupId = artifactGroup
            artifactId = artifactName
            from(components["java"])
        }
    }
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
