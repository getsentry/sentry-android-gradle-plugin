package io.sentry.android.gradle.util

import com.android.builder.model.Version
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.util.GradleVersion

internal object AgpVersions {
    val CURRENT: SemVer = SemVer.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION)
    val VERSION_7_1_2: SemVer = SemVer.parse("7.1.2")
    val VERSION_7_0_0: SemVer = SemVer.parse("7.0.0")
    val VERSION_7_4_0: SemVer = SemVer.parse("7.4.0-rc01")
    val isAGP74: Boolean get() = isAGP74(CURRENT)
    fun isAGP74(current: SemVer) = current >= VERSION_7_4_0
}

internal object GradleVersions {
    val CURRENT: SemVer = SemVer.parse(GradleVersion.current().version)
    val VERSION_7_4: SemVer = SemVer.parse("7.4")
    val VERSION_7_5: SemVer = SemVer.parse("7.5")
}

internal object SentryVersions {
    internal val VERSION_DEFAULT = SemVer()
    internal val VERSION_PERFORMANCE = SemVer(4, 0, 0)
    internal val VERSION_OKHTTP = SemVer(5, 0, 0)
    internal val VERSION_FILE_IO = SemVer(5, 5, 0)
    internal val VERSION_COMPOSE = SemVer(6, 7, 0)
    internal val VERSION_LOGCAT = SemVer(6, 17, 0)
}

internal object SentryModules {
    internal val SENTRY_ANDROID_CORE = DefaultModuleIdentifier.newId(
        "io.sentry",
        "sentry-android-core"
    )
    internal val SENTRY_ANDROID_OKHTTP = DefaultModuleIdentifier.newId(
        "io.sentry",
        "sentry-android-okhttp"
    )
    internal val SENTRY_ANDROID_COMPOSE = DefaultModuleIdentifier.newId(
        "io.sentry",
        "sentry-compose-android"
    )
}
