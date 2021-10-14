pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
            if (requested.id.id == "io.sentry.android.gradle") {
                useModule("io.sentry:sentry-android-gradle-plugin:${requested.version}")
            }
        }
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = ("sentry-android-gradle-plugin-composite-build")

include(":examples:android-gradle")
include(":examples:android-gradle-kts")
include(":examples:android-ndk")
include(":examples:android-room")
include(":examples:android-room-lib")
includeBuild("plugin-build") {
    dependencySubstitution {
        substitute(module("io.sentry:sentry-android-gradle-plugin")).using(project(":"))
    }
}
