pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://api.modrinth.com/maven")
        mavenCentral()
    }
}

rootProject.name = "TropimonTeamBuilder"
