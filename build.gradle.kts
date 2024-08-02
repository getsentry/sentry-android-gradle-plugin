plugins {
    kotlin("jvm") version BuildPluginsVersion.KOTLIN apply false
    id("com.android.application") version BuildPluginsVersion.AGP apply false
    id("com.diffplug.spotless") version BuildPluginsVersion.SPOTLESS
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
                ktfmt(BuildPluginsVersion.KTFMT).googleStyle()
                targetExclude("**/generated/**")
            }
            kotlinGradle {
                ktfmt(BuildPluginsVersion.KTFMT).googleStyle()
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
