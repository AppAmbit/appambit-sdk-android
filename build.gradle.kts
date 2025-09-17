// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    id ("io.github.gradle-nexus.publish-plugin") version ("1.1.0")
}

nexusPublishing {
    repositories {
        sonatype {
            username = System.getenv("OSSRH_USERNAME")
            password = System.getenv("OSSRH_PASSWORD")

            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}