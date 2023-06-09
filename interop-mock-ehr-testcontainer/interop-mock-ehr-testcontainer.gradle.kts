plugins {
    id("com.projectronin.interop.gradle.server-publish")
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.junit")
}

dependencies {
    api(platform(libs.testcontainers.bom))
    api("org.testcontainers:testcontainers")
    implementation("org.testcontainers:mysql")
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    testImplementation(libs.mockk)
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation(libs.mysql.connector.java)
}
