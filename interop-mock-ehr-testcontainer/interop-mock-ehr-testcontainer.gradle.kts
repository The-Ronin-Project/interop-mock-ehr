plugins {
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.publish")
}

dependencies {
    implementation(project(":interop-mock-ehr"))

    implementation("org.testcontainers:junit-jupiter:1.16.3")
}
