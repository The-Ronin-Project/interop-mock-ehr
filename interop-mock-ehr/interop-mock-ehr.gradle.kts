plugins {
    alias(libs.plugins.interop.gradle.spring.boot)
    alias(libs.plugins.interop.gradle.junit)
}

dependencies {
    implementation(platform(libs.spring.boot.parent))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.mysql.connector.java)
    implementation(libs.protobuf.java)
    implementation(libs.interop.ehr)
    implementation(libs.interop.ehr.cerner)
    implementation(libs.interop.ehr.epic)
    implementation(libs.interop.fhir)
    implementation(libs.json.patch)
    implementation(libs.apache.commons.text)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.bundles.hapi.fhir)
    implementation(libs.bundles.hapi.hl7v2)
    implementation(libs.bundles.springdoc.openapi)

    testImplementation(libs.mockk)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:mysql")

    // Needed to format logs for DataDog
    runtimeOnly(libs.logstash.logback.encoder)
}
