rootProject.name = "geogeometry"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.60.5"
}

refreshVersions {
    extraArtifactVersionKeyRules(file("version_key_rules.txt"))
}
