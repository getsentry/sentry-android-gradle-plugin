import org.gradle.util.VersionNumber

object BuildPluginsVersion {
    val AGP = System.getenv("VERSION_AGP") ?: "7.4.0"

	// proguard does not support AGP 8 yet
    fun isProguardApplicable(): Boolean = VersionNumber.parse(AGP).major < 8
}

object LibsVersion {
    const val SDK_VERSION = 33
    const val MIN_SDK_VERSION = 21
}

object Libs {
    fun agp(version: String) = "com.android.tools.build:gradle:$version"
    val AGP = "com.android.tools.build:gradle:${BuildPluginsVersion.AGP}"
    const val PROGUARD = "com.guardsquare:proguard-gradle:7.1.0"
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
