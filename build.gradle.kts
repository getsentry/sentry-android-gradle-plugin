plugins {
    kotlin("jvm") version BuildPluginsVersion.KOTLIN apply false
    id("com.android.application") version BuildPluginsVersion.AGP apply false
    id("org.jlleitschuh.gradle.ktlint") version BuildPluginsVersion.KTLINT
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("org.jlleitschuh.gradle.ktlint")
    }

    ktlint {
        debug.set(false)
        verbose.set(true)
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        enableExperimentalRules.set(true)
        filter {
            exclude("**/generated/**")
            include("**/kotlin/**")
        }
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
    dependsOn(gradle.includedBuild("plugin-build").task(":clean"))
}

tasks.register("preMerge") {
    description = "Runs all the tests/verification tasks on both top level and included build."

    dependsOn(":sentry-kotlin-compiler-plugin:publishAllPublicationsToMavenTestRepoRepository")
    dependsOn(":examples:android-gradle:check")
    dependsOn(":examples:android-gradle-kts:check")
    dependsOn(":examples:android-ndk:check")
    dependsOn(":examples:android-instrumentation-sample:check")
    dependsOn(":examples:android-room-lib:check")
    dependsOn(gradle.includedBuild("plugin-build").task(":check"))
}

tasks.getByName("ktlintFormat") {
    dependsOn(gradle.includedBuild("plugin-build").task(":ktlintFormat"))
}

tasks.getByName("ktlintCheck") {
    dependsOn(gradle.includedBuild("plugin-build").task(":ktlintCheck"))
}
