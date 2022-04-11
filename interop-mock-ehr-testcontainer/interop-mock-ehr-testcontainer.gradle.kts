plugins {
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.publish")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    implementation(project(":interop-mock-ehr"))

    implementation("org.testcontainers:junit-jupiter:1.16.3")
    implementation("org.testcontainers:mysql:1.16.3")
}
