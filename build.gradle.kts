import com.android.build.gradle.AppExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

    tasks.withType<KotlinCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()

        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    plugins.withId("com.android.application") {
        val ext = extensions.getByName("android") as AppExtension
        ext.compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
    dependsOn(gradle.includedBuild("plugin-build").task(":clean"))
}

tasks.register("preMerge") {
    description = "Runs all the tests/verification tasks on both top level and included build."

    dependsOn(":examples:android-gradle:check")
    dependsOn(":examples:android-gradle-kts:check")
    dependsOn(":examples:android-ndk:check")
    dependsOn(gradle.includedBuild("plugin-build").task(":check"))
}

tasks.register("closeAndReleaseRepository") {
    description = "Runs closeAndReleaseRepository for plugin-build"

    dependsOn(gradle.includedBuild("plugin-build").task(":closeAndReleaseRepository"))
}

tasks.getByName("ktlintFormat") {
    dependsOn(gradle.includedBuild("plugin-build").task(":ktlintFormat"))
}
