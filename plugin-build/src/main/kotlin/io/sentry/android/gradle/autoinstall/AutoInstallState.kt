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
    var installSqlite: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installFragment: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installTimber: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installCompose: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installSpring: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installLogback: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installLog4j2: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installJdbc: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installGraphql: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var installKotlinExtensions: Boolean = false

    override fun toString(): String {
        return "AutoInstallState(sentryVersion='$sentryVersion', " +
            "installOkHttp=$installOkHttp, " +
            "installSqlite=$installSqlite, " +
            "installFragment=$installFragment, " +
            "installTimber=$installTimber, " +
            "installCompose=$installCompose, " +
            "installSpring=$installSpring, " +
            "installLogback=$installLogback, " +
            "installLog4j2=$installLog4j2, " +
            "installJdbc=$installJdbc), " +
            "installGraphql=$installGraphql), " +
            "installKotlinExtensions=$installKotlinExtensions)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AutoInstallState

        if (sentryVersion != other.sentryVersion) return false
        if (installOkHttp != other.installOkHttp) return false
        if (installSqlite != other.installSqlite) return false
        if (installFragment != other.installFragment) return false
        if (installTimber != other.installTimber) return false
        if (installCompose != other.installCompose) return false
        if (installSpring != other.installSpring) return false
        if (installLogback != other.installLogback) return false
        if (installLog4j2 != other.installLog4j2) return false
        if (installJdbc != other.installJdbc) return false
        if (installGraphql != other.installGraphql) return false
        if (installKotlinExtensions != other.installKotlinExtensions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sentryVersion.hashCode()
        result = 31 * result + installOkHttp.hashCode()
        result = 31 * result + installSqlite.hashCode()
        result = 31 * result + installFragment.hashCode()
        result = 31 * result + installTimber.hashCode()
        result = 31 * result + installCompose.hashCode()
        result = 31 * result + installSpring.hashCode()
        result = 31 * result + installLogback.hashCode()
        result = 31 * result + installLog4j2.hashCode()
        result = 31 * result + installJdbc.hashCode()
        result = 31 * result + installGraphql.hashCode()
        result = 31 * result + installKotlinExtensions.hashCode()
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
