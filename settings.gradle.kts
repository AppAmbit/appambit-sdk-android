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
include(":appambit-testapp")
include(":appambit-sdk")
include(":appambit-kotlin-testapp")
include(":apps:app-push-notifications")
project(":apps:app-push-notifications").projectDir = file("apps/app-push-notifications")
include(":push:appambit-sdk-push-notifications")
project(":push:appambit-sdk-push-notifications").projectDir = file("push/appambit-sdk-push-notifications")
