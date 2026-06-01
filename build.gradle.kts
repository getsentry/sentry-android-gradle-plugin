buildscript {
  dependencies { classpath("org.apache.commons:commons-compress:1.28.0") }
  if (BuildPluginsVersion.AGP.substringBefore(".").toInt() < 8) {
    // AGP 7.x has troubles with compileSdk 34 due to some R8 shenanigans, so we have to use a newer
    // version of R* here
    dependencies { classpath("com.android.tools:r8:8.11.18") }
  }
}

plugins {
  alias(libs.plugins.kotlin) version BuildPluginsVersion.KOTLIN apply false
  alias(libs.plugins.kotlinAndroid) version BuildPluginsVersion.KOTLIN apply false
  alias(libs.plugins.kapt) version BuildPluginsVersion.KOTLIN apply false
  alias(libs.plugins.ksp) version BuildPluginsVersion.KSP apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.androidApplication) version BuildPluginsVersion.AGP apply false
  alias(libs.plugins.androidLibrary) version BuildPluginsVersion.AGP apply false
  alias(libs.plugins.spotless)
}

allprojects {
  apply { plugin("com.diffplug.spotless") }

  if (name != "examples") {
    spotless {
      if (name != rootProject.name) {
        kotlin {
          ktfmt(libs.versions.ktfmt.get()).googleStyle()
          target("**/*.kt")
        }
      }
      kotlinGradle {
        ktfmt(libs.versions.ktfmt.get()).googleStyle()
        target("**/*.kts")
      }
    }
  }
}

tasks.withType<Delete>().configureEach {
  delete(rootProject.buildDir)
  gradle.includedBuilds.forEach { dependsOn(it.task(":clean")) }
}

tasks.register("integrationTest") {
  group = "verification"
  description = "Runs the integration tests"

  dependsOn(
    gradle
      .includedBuild("sentry-kotlin-compiler-plugin")
      .task(":publishAllPublicationsToMavenTestRepoRepository")
  )
  dependsOn(gradle.includedBuild("plugin-build").task(":integrationTest"))
}

tasks.register("preMerge") {
  description = "Runs all the tests/verification tasks on both top level and included build."

  dependsOn(":check")
  dependsOn(gradle.includedBuild("sentry-kotlin-compiler-plugin").task(":check"))
  dependsOn(
    gradle
      .includedBuild("sentry-kotlin-compiler-plugin")
      .task(":publishAllPublicationsToMavenTestRepoRepository")
  )

  dependsOn(":examples:android-gradle:check")
  dependsOn(":examples:android-gradle-kts:check")
  dependsOn(":examples:android-ndk:check")
  dependsOn(":examples:android-instrumentation-sample:check")
  dependsOn(":examples:android-room-lib:check")
  dependsOn(gradle.includedBuild("plugin-build").task(":check"))
}

tasks.named("spotlessCheck") {
  gradle.includedBuilds.forEach { dependsOn(it.task(":spotlessCheck")) }
}

tasks.named("spotlessApply") {
  gradle.includedBuilds.forEach { dependsOn(it.task(":spotlessApply")) }
}

tasks.named("assemble") { gradle.includedBuilds.forEach { dependsOn(it.task(":assemble")) } }
