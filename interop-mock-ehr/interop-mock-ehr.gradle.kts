plugins {
    alias(libs.plugins.interop.gradle.spring.boot)
    alias(libs.plugins.interop.gradle.junit)
}

dependencies {
    configurations.all {
        resolutionStrategy {
            force(libs.jackson.core)
            force(libs.thymeleaf)
            force(libs.mockk)
            force(libs.jakarta.servlet)
            force(libs.spring.boot.parent)
            force(libs.spring.framework.bom)
        }
    }

    implementation(enforcedPlatform(libs.spring.boot.parent))
    implementation(enforcedPlatform(libs.spring.framework.bom))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.mysql.connector.java)
    implementation(libs.protobuf.java)
    implementation(libs.interop.ehr) {
        exclude(group = "org.springdoc")
    }
    implementation(libs.interop.ehr.cerner) {
        exclude(group = "org.springdoc")
    }
    implementation(libs.interop.ehr.epic) {
        exclude(group = "org.springdoc")
    }
    implementation(libs.interop.fhir)
    implementation(libs.jakarta.servlet)
    implementation(libs.json.patch)
    implementation(libs.apache.commons.text)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.bundles.hapi.fhir)
    implementation(libs.bundles.hapi.hl7v2)
    implementation(libs.bundles.springdoc.openapi)

    runtimeOnly(libs.thymeleaf)

    testImplementation(libs.mockk)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:mysql")

    // Needed to format logs for DataDog
    runtimeOnly(libs.logstash.logback.encoder)
}
