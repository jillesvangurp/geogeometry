rootProject.name = "geogeometry"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.40.2"
}

refreshVersions {
    extraArtifactVersionKeyRules(file("version_key_rules.txt"))
}
