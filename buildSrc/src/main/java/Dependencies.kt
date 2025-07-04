import org.gradle.util.internal.VersionNumber

object BuildPluginsVersion {
    val AGP = System.getenv("VERSION_AGP") ?: "8.10.1"
    val GROOVY_REDISTRIBUTED = System.getenv("VERSION_GROOVY") ?: "1.7.1"
}

object LibsVersion {
    const val SDK_VERSION = 33
    const val MIN_SDK_VERSION = 21
}

object Libs {
    fun agp(version: String) = "com.android.tools.build:gradle:$version"
    val AGP = "com.android.tools.build:gradle:${BuildPluginsVersion.AGP}"
}

object CI {
    const val SENTRY_SDKS_DSN = "https://dd1f82ad30a331bd7def2a0dce926c6e@o447951.ingest.sentry.io/4506031723446272"
    fun canAutoUpload(): Boolean {
        return System.getenv("AUTO_UPLOAD").toBoolean() &&
                !System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty()
    }
}

object Samples {
    object SpringBoot {
        val kotlinStdLib = "stdlib-jdk8"
    }
}
