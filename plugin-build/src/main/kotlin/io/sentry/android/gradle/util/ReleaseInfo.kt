package io.sentry.android.gradle.util

import java.io.Serializable

data class ReleaseInfo(
    val applicationId: String,
    val versionName: String,
    val versionCode: Int? = null
) : Serializable
