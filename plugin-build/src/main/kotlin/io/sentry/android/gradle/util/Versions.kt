package io.sentry.android.gradle.util

import com.android.builder.model.Version
import org.gradle.util.internal.VersionNumber

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION)
    val VERSION_7_2_0_alpha06: VersionNumber = VersionNumber.parse("7.2.0-alpha06")
}
