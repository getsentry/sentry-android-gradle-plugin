package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.SentryPlugin.Companion.SENTRY_SDK_VERSION
import java.io.Serializable

object AutoInstallState : Serializable {
    @get:Synchronized
    @set:Synchronized
    var sentryVersion: String = SENTRY_SDK_VERSION

    @get:Synchronized
    @set:Synchronized
    var installOkHttp: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installFragment: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installTimber: Boolean = false
}
