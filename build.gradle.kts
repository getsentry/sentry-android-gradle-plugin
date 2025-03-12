plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.androidApplication) version BuildPluginsVersion.AGP apply false
    alias(libs.plugins.androidLibrary) version BuildPluginsVersion.AGP apply false
    alias(libs.plugins.spotless)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("com.diffplug.spotless")
    }

    if (name != "examples") {
        spotless {
            kotlin {
                ktfmt(libs.versions.ktfmt.get()).googleStyle()
                targetExclude("**/generated/**")
            }
        }
    }
}

tasks.withType<Delete>().configureEach {
    delete(rootProject.buildDir)
    dependsOn(gradle.includedBuild("plugin-build").task(":clean"))
    dependsOn(gradle.includedBuild("sentry-kotlin-compiler-plugin").task(":clean"))
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs the integration tests"

    dependsOn(gradle.includedBuild("sentry-kotlin-compiler-plugin").task(":publishAllPublicationsToMavenTestRepoRepository"))
    dependsOn(gradle.includedBuild("plugin-build").task(":integrationTest"))
}

tasks.register("preMerge") {
    description = "Runs all the tests/verification tasks on both top level and included build."

    dependsOn(gradle.includedBuild("sentry-kotlin-compiler-plugin").task(":check"))
    dependsOn(gradle.includedBuild("sentry-kotlin-compiler-plugin").task(":publishAllPublicationsToMavenTestRepoRepository"))

    dependsOn(":examples:android-gradle:check")
    dependsOn(":examples:android-gradle-kts:check")
    dependsOn(":examples:android-ndk:check")
    dependsOn(":examples:android-instrumentation-sample:check")
    dependsOn(":examples:android-room-lib:check")
    dependsOn(gradle.includedBuild("plugin-build").task(":check"))
}

tasks.getByName("spotlessCheck") {
    dependsOn(gradle.includedBuild("sentry-kotlin-compiler-plugin").task(":spotlessCheck"))
    dependsOn(gradle.includedBuild("plugin-build").task(":spotlessCheck"))
}

tasks.getByName("spotlessApply") {
    dependsOn(gradle.includedBuild("sentry-kotlin-compiler-plugin").task(":spotlessApply"))
    dependsOn(gradle.includedBuild("plugin-build").task(":spotlessApply"))
}
