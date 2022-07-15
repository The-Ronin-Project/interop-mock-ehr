import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `maven-publish`
    id("org.springframework.boot")
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.junit")
}

dependencies {
    implementation(platform(libs.spring.boot.parent))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.mysql.connector.java)
    implementation(libs.protobuf.java)
    implementation(libs.interop.ehr.epic)
    implementation(libs.interop.fhir)
    implementation(libs.json.patch)

    implementation(libs.bundles.hapi.fhir)
    implementation(libs.bundles.hapi.hl7v2)
    implementation(libs.bundles.springdoc.openapi)

    testImplementation(libs.mockk)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:mysql")
}

publishing {
    repositories {
        maven {
            name = "nexus"
            credentials {
                username = System.getenv("NEXUS_USER")
                password = System.getenv("NEXUS_TOKEN")
            }
            url = if (project.version.toString().endsWith("SNAPSHOT")) {
                uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
            } else {
                uri("https://repo.devops.projectronin.io/repository/maven-releases/")
            }
        }
    }
    publications {
        create<MavenPublication>("bootJava") {
            artifact(tasks.getByName("bootJar"))
        }
    }
}
// usually this is pulled in via interop-gradle.publish
tasks.register("install") {
    dependsOn(tasks.publishToMavenLocal)
}

tasks.withType(Test::class) {
    testLogging {
        events(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
    }
}
