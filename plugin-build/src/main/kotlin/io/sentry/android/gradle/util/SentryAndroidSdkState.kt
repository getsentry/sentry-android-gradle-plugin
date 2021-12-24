package io.sentry.android.gradle.util

import java.io.Serializable

enum class SentryAndroidSdkState(val minVersion: String) : Serializable {
    MISSING(""),

    PERFORMANCE("4.0.0"),

    FILE_IO("5.5.0");

    fun isAtLeast(state: SentryAndroidSdkState): Boolean = this.ordinal >= state.ordinal

    companion object {
        val semverRegex =
            Regex("((([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?)(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?)")

        fun from(semVer: String): SentryAndroidSdkState =
            when {
                semVer == "unspecified" -> MISSING
                !semverRegex.matches(semVer) -> error("Unknown version $semVer of sentry-android")
                semVer < PERFORMANCE.minVersion -> MISSING
                semVer >= PERFORMANCE.minVersion && semVer < FILE_IO.minVersion -> PERFORMANCE
                semVer >= FILE_IO.minVersion -> FILE_IO
                else -> error("Unknown version $semVer of sentry-android")
            }
    }
}
