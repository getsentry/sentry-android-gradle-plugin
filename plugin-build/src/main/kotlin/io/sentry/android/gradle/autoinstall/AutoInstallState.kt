package io.sentry.android.gradle.autoinstall

import io.sentry.android.gradle.SentryPlugin.Companion.SENTRY_SDK_VERSION
import java.io.Serializable
import org.gradle.api.invocation.Gradle

class AutoInstallState private constructor() : Serializable {

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

    override fun toString(): String {
        return "AutoInstallState(sentryVersion='$sentryVersion', " +
            "installOkHttp=$installOkHttp, " +
            "installFragment=$installFragment, " +
            "installTimber=$installTimber)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AutoInstallState

        if (sentryVersion != other.sentryVersion) return false
        if (installOkHttp != other.installOkHttp) return false
        if (installFragment != other.installFragment) return false
        if (installTimber != other.installTimber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sentryVersion.hashCode()
        result = 31 * result + installOkHttp.hashCode()
        result = 31 * result + installFragment.hashCode()
        result = 31 * result + installTimber.hashCode()
        return result
    }

    // We can't use Kotlin object because we need new instance on each Gradle rebuild
    // But if we're inside Gradle daemon, Kotlin object will be shared between builds
    companion object {
        @field:Volatile
        private var instance: AutoInstallState? = null

        @JvmStatic
        @Synchronized
        fun getInstance(gradle: Gradle? = null): AutoInstallState {
            if (instance != null) {
                return instance!!
            }

            val state = AutoInstallState()
            instance = state

            if (gradle != null) {
                BuildFinishedListenerService.getInstance(gradle).onClose { instance = null }
            }

            return state
        }
    }
}
