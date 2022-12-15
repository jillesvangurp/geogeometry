rootProject.name = "geogeometry"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.51.0"
}

refreshVersions {
    extraArtifactVersionKeyRules(file("version_key_rules.txt"))
}
