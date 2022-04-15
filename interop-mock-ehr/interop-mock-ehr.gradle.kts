plugins {
    `maven-publish`
    id("org.springframework.boot")
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.mockk")
}

dependencies {
    implementation(platform(libs.spring.boot.parent))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.mysql.connector.java)
    implementation(libs.protobuf.java)
    implementation(libs.interop.ehr.epic)
    implementation(libs.interop.fhir)

    implementation(libs.bundles.hapi.fhir)

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
