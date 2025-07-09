pluginManagement {
    repositories {
        google()            // 🔥 Android plugin'leri burada bulunur
        mavenCentral()      // 🔥 Diğer Kotlin/Java plugin'leri burada bulunur
        gradlePluginPortal() // 🔥 Kotlin DSL plugin'lerini destekler
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "arduinostepmotorkontrol"
include(":app")
