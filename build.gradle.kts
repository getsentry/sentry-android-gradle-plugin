plugins {
    kotlin("jvm") version BuildPluginsVersion.KOTLIN apply false
    id("com.android.application") version BuildPluginsVersion.AGP apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
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
