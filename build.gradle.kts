plugins {
    id("com.projectronin.interop.gradle.junit") apply false
    id("com.projectronin.interop.gradle.publish") apply false
    id("com.projectronin.interop.gradle.spring") apply false
    id("com.projectronin.interop.gradle.version")
}

subprojects {
    // The task filter below does not appear to fire unless we include something being applied.
    // In other builds, it's publish. But we're not using publish in all subprojects, so I've used base.
    apply(plugin = "com.projectronin.interop.gradle.base")

    // Disable releases hub from running on the subprojects. Main project will handle it all.
    tasks.filter { it.group.equals("releases hub", ignoreCase = true) }.forEach { it.enabled = false }
}
