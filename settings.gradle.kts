pluginManagement {
    repositories {
        mavenLocal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
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

rootProject.name = "AppAmbit Testing App"
// SDK
include(":appambit-sdk")

// APPS
include(":appambit-testapp")
project(":appambit-testapp").projectDir = file("samples/java-app")

include(":appambit-kotlin-testapp")
project(":appambit-kotlin-testapp").projectDir = file("samples/kotlin-app")

// TESTS
include(":AppAmbitSdkTest")
project(":AppAmbitSdkTest").projectDir = file("tests/AppAmbitSdkTest")