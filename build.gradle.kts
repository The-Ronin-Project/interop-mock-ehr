plugins {
    alias(libs.plugins.interop.gradle.junit) apply false
    alias(libs.plugins.interop.gradle.server.publish) apply false
    alias(libs.plugins.interop.gradle.spring) apply false
    alias(libs.plugins.interop.gradle.spring.boot) apply false
    alias(libs.plugins.interop.gradle.server.version)
    alias(libs.plugins.interop.version.catalog)
    alias(libs.plugins.interop.gradle.sonarqube)
}

subprojects {
    // The task filter below does not appear to fire unless we include something being applied.
    // In other builds, it's publish. But we're not using publish in all subprojects, so I've used base.
    apply(plugin = "com.projectronin.interop.gradle.base")
}
