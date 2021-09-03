rootProject.name = "geogeometry"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.11.0"
////                            # available:"0.20.0"
}

refreshVersions {
    extraArtifactVersionKeyRules(file("version_key_rules.txt"))
}
