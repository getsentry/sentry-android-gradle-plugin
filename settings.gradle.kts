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
  // The pinned KSP (KSP1, 2.1.0-1.0.29) is bound to the Kotlin 2.1 compiler and rejects the
  // newer Kotlin versions the test matrix picks (e.g. 2.3.21). When the matrix overrides the
  // Kotlin version via VERSION_KOTLIN, swap in the latest KSP2 release, which is decoupled
  // from the Kotlin compiler and supports language version 2.0+.
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "com.google.devtools.ksp" && System.getenv("VERSION_KOTLIN") != null) {
        useVersion("2.3.7")
      }
    }
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
