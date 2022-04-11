plugins {
    `maven-publish`
    id("org.springframework.boot") version "2.4.5"
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.mockk")
}

var hapiversion = "5.6.0"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-parent:2.6.4"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("mysql:mysql-connector-java:8.0.28")
    implementation("com.google.protobuf:protobuf-java:3.19.4")
    implementation("com.projectronin.interop.ehr:interop-ehr-epic:1.0.0-SNAPSHOT")
    implementation("com.projectronin.interop.fhir:interop-fhir:1.0.0-SNAPSHOT")

    implementation("ca.uhn.hapi.fhir", "hapi-fhir-base", hapiversion)
    implementation("ca.uhn.hapi.fhir", "hapi-fhir-server", hapiversion)
    implementation("ca.uhn.hapi.fhir", "hapi-fhir-structures-r4", hapiversion)

    testImplementation("org.testcontainers:junit-jupiter:1.16.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:mysql:1.16.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
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
