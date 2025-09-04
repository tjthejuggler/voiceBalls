// File: settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // This is the server that your PING command can now successfully reach.
        maven { url = uri("https://maven.picovoice.ai") }
    }
}
rootProject.name = "voiceBalls"
include(":app")