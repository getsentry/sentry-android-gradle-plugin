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
        jcenter()
    }
}

rootProject.name = ("sentry-android-gradle-plugin-composite-build")

include(":examples:android-gradle")
include(":examples:android-gradle-kts")
includeBuild("plugin-build") {
    dependencySubstitution {
        substitute(module("io.sentry:sentry-android-gradle-plugin")).with(project(":"))
    }
}
