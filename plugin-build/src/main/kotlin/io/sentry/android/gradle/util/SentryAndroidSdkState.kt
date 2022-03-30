package io.sentry.android.gradle.util

import java.io.Serializable

enum class SentryAndroidSdkState(val minVersion: SemVer) : Serializable {
    MISSING(SemVer()),

    PERFORMANCE(SemVer(4, 0, 0)),

    OKHTTP(SemVer(5, 0, 0)),

    FILE_IO(SemVer(5, 5, 0));

    fun isAtLeast(state: SentryAndroidSdkState): Boolean = this.ordinal >= state.ordinal

    companion object {
        fun from(version: String): SentryAndroidSdkState {
            if (version == "unspecified") {
                return MISSING
            }

            val semVer = SemVer.parse(version)
            return when {
                semVer < PERFORMANCE.minVersion -> MISSING
                semVer >= PERFORMANCE.minVersion && semVer < OKHTTP.minVersion -> PERFORMANCE
                semVer >= OKHTTP.minVersion && semVer < FILE_IO.minVersion -> OKHTTP
                semVer >= FILE_IO.minVersion -> FILE_IO
                else -> error("Unknown version $version of sentry-android")
            }
        }
    }
}
