plugins {
    val agpVersion = System.getenv("VERSION_AGP") ?: BuildPluginsVersion.AGP

    kotlin("jvm") version BuildPluginsVersion.KOTLIN apply false
    id("com.android.application") version agpVersion apply false
    id("org.jlleitschuh.gradle.ktlint") version BuildPluginsVersion.KTLINT
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}

subprojects {
    apply {
        plugin("org.jlleitschuh.gradle.ktlint")
    }

    ktlint {
        debug.set(false)
        version.set(Versions.KTLINT)
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

    dependsOn(":examples:android-gradle:check")
    dependsOn(":examples:android-gradle-kts:check")
    dependsOn(gradle.includedBuild("plugin-build").task(":check"))
}
