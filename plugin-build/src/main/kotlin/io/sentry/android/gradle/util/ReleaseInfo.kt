package io.sentry.android.gradle.util

data class ReleaseInfo(
    val applicationId: String,
    val versionName: String,
    val versionCode: Int? = null
)
