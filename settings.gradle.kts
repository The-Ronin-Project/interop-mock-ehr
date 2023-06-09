rootProject.name = "interop-mock-ehr-build"

include("interop-mock-ehr")
include("interop-mock-ehr-testcontainer")

for (project in rootProject.children) {
    project.buildFileName = "${project.name}.gradle.kts"
}

pluginManagement {
    plugins {
        id("com.projectronin.interop.gradle.base") version "3.1.0"
        id("com.projectronin.interop.gradle.junit") version "3.1.0"
        id("com.projectronin.interop.gradle.server-publish") version "3.1.0"
        id("com.projectronin.interop.gradle.server-version") version "3.1.0"
        id("com.projectronin.interop.gradle.spring") version "3.1.0"
        id("com.projectronin.interop.gradle.spring-boot") version "3.1.0"
    }

    repositories {
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
            mavenContent {
                snapshotsOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-releases/")
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
            mavenContent {
                releasesOnly()
            }
        }
        mavenLocal()
        gradlePluginPortal()
    }
}
