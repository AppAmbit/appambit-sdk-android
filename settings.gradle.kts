pluginManagement {
    repositories {
        mavenLocal()
        google {
            content {
                includeGroupByRegex("com.android.*")
                includeGroupByRegex("com.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "AppAmbit"
// SDK
include(":appambit-sdk")

// APPS
include(":java-app")
project(":java-app").projectDir = file("samples/java-app")
include(":kotlin-app")
project(":kotlin-app").projectDir = file("samples/kotlin-app")
include(":push:appambit-sdk-push-notifications")
project(":push:appambit-sdk-push-notifications").projectDir = file("push/appambit-sdk-push-notifications")

// TESTS
include(":AppAmbitSdkTest")
project(":AppAmbitSdkTest").projectDir = file("tests/AppAmbitSdkTest")
