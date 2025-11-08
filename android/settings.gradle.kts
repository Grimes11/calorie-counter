pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()        // <-- required for Firebase/AndroidX
        mavenCentral()
    }
}
rootProject.name = "calorie-counter"
include(":app")
