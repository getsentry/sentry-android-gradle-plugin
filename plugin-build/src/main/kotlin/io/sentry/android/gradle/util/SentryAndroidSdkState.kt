package io.sentry.android.gradle.util

import java.io.Serializable

enum class SentryAndroidSdkState(val minVersion: String) : Serializable {
    MISSING(""),

    PERFORMANCE("4.0.0"),

    FILE_IO("5.5.0");

    fun isAtLeast(state: SentryAndroidSdkState): Boolean = this.ordinal >= state.ordinal

    companion object {
        fun from(semVer: String): SentryAndroidSdkState =
            when {
                semVer < PERFORMANCE.minVersion -> MISSING
                semVer >= PERFORMANCE.minVersion && semVer < FILE_IO.minVersion -> PERFORMANCE
                semVer >= FILE_IO.minVersion -> FILE_IO
                else -> error("Unknown version $semVer of sentry-android")
            }
    }
}
