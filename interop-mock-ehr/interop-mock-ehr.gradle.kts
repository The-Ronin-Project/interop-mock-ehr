plugins {
    id("com.projectronin.interop.gradle.spring-boot")
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
