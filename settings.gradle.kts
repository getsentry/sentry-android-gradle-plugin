pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
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

includeBuild("sentry-kotlin-compiler-plugin")
