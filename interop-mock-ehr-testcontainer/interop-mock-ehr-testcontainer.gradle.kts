plugins {
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.publish")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    implementation(platform(libs.testcontainers.bom))
    implementation("org.testcontainers:testcontainers")
    implementation("org.testcontainers:mysql")

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation(libs.mysql.connector.java)
}
