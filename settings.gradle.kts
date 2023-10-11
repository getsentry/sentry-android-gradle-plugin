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
    }
}

rootProject.name = ("sentry-android-gradle-plugin-composite-build")

include(":examples:android-gradle")
include(":examples:android-gradle-kts")
include(":examples:android-guardsquare-proguard")
include(":examples:android-ndk")
include(":examples:android-instrumentation-sample")
include(":examples:android-room-lib")
include(":examples:spring-boot-sample")
includeBuild("plugin-build") {
    dependencySubstitution {
        substitute(module("io.sentry:sentry-android-gradle-plugin")).using(project(":"))
    }
}
includeBuild("sentry-kotlin-compiler-plugin")
