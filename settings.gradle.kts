pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
    maven {
      url = uri("https://storage.googleapis.com/r8-releases/raw")
      content { includeGroup("com.android.tools") }
    }
  }
}

plugins {
  id("com.gradle.develocity") version "4.5.0"
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.8.0"
}

val isCI = System.getenv("CI") != null
val gradleTOUAgree = "true" == providers.gradleProperty("gradleTOUAgree").orNull

develocity {
  buildScan {
    termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
    termsOfUseAgree.set(if (isCI || gradleTOUAgree) "yes" else "no")
    uploadInBackground = !isCI
    // we can't pass the condition directly inside the onlyIf because of a CC bug in Gradle 8.X
    // (this is fixed in 9.X)
    if (!isCI && !gradleTOUAgree) {
      publishing.onlyIf { false }
    }
    obfuscation { ipAddresses { addresses -> addresses.map { _ -> "0.0.0.0" } } }
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
  }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "sentry-android-gradle-plugin-composite-build"

include(":examples:android-gradle")

include(":examples:android-gradle-kts")

// Configuring this sample makes AGP download and set up the NDK, which isn't available when
// Renovate regenerates the dependency lockfile/verification metadata and would fail the run.
// The sample is never needed to resolve dependencies, so skip it on those runs. Renovate
// always invokes Gradle with --write-locks/--update-locks and --dependency-verification
// lenient; normal dev and CI builds don't, so they still build the sample.
val isDependencyResolutionRun =
  startParameter.isWriteDependencyLocks ||
    startParameter.lockedDependenciesToUpdate.isNotEmpty() ||
    startParameter.dependencyVerificationMode ==
      org.gradle.api.artifacts.verification.DependencyVerificationMode.LENIENT

if (!isDependencyResolutionRun) {
  include(":examples:android-ndk")
}

include(":examples:android-instrumentation-sample")

include(":examples:android-room-lib")

include(":examples:spring-boot-sample")

include(":examples:multi-module-sample")

include(":examples:multi-module-sample:spring-boot-in-multi-module-sample")

include(":examples:multi-module-sample:spring-boot-in-multi-module-sample2")

includeBuild("plugin-build")

// this is needed so we can use kotlin-compiler-plugin directly in the sample app without publishing
includeBuild("sentry-kotlin-compiler-plugin") {
  dependencySubstitution {
    substitute(module("io.sentry:sentry-kotlin-compiler-plugin")).using(project(":"))
  }
}

includeBuild("sentry-snapshots-runtime") {
  dependencySubstitution {
    substitute(module("io.sentry:sentry-snapshots-runtime")).using(project(":"))
  }
}
