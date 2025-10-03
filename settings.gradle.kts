pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "com.android.application") {
        useModule("com.android.tools.build:gradle:${requested.version}")
      }
      if (requested.id.id == "io.sentry.android.gradle") {
        useModule("io.sentry:sentry-android-gradle-plugin:${requested.version}")
      }
      if (requested.id.id == "io.sentry.kotlin.compiler.gradle") {
        useModule("io.sentry:sentry-android-gradle-plugin:${requested.version}")
      }
      if (requested.id.id == "io.sentry.jvm.gradle") {
        useModule("io.sentry:sentry-android-gradle-plugin:${requested.version}")
      }
    }
  }
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
    maven {
      url = uri("https://storage.googleapis.com/r8-releases/raw")
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

include(":examples:android-guardsquare-proguard")

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
