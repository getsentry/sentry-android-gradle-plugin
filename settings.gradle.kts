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
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.6.0"
}

develocity {
  buildScan {
    termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
    termsOfUseAgree.set("yes")
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
  }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = ("sentry-android-gradle-plugin-composite-build")

// Renovate updates dependencies by invoking the root wrapper with --write-locks/--update-locks
// and --dependency-verification lenient, none of which normal dev and CI builds pass. A root
// invocation can't refresh anything anyway: the only lockfile and verification metadata live in
// plugin-build, and scripts/relock-plugin-build.sh regenerates those with -p plugin-build. But
// configuring this build does break those runs, in two ways: the android-ndk sample makes AGP
// download an NDK that Renovate's environment doesn't have, and --write-verification-metadata
// calls getRootProject() on every build in the composite, which throws for the included builds
// that a root `dependencies` task never configures. So configure nothing on those runs.
val isDependencyResolutionRun =
  startParameter.isWriteDependencyLocks ||
    startParameter.lockedDependenciesToUpdate.isNotEmpty() ||
    startParameter.dependencyVerificationMode ==
      org.gradle.api.artifacts.verification.DependencyVerificationMode.LENIENT

if (!isDependencyResolutionRun) {
  include(":examples:android-gradle")

  include(":examples:android-gradle-kts")

  include(":examples:android-ndk")

  include(":examples:android-instrumentation-sample")

  include(":examples:android-room-lib")

  include(":examples:spring-boot-sample")

  include(":examples:multi-module-sample")

  include(":examples:multi-module-sample:spring-boot-in-multi-module-sample")

  include(":examples:multi-module-sample:spring-boot-in-multi-module-sample2")

  includeBuild("plugin-build")

  // this is needed so we can use kotlin-compiler-plugin directly in the sample app without
  // publishing
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
}
