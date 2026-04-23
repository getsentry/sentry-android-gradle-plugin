object BuildPluginsVersion {
    val AGP = System.getenv("VERSION_AGP") ?: "8.10.1"
    val KOTLIN = System.getenv("VERSION_KOTLIN") ?: "1.8.20"
    // KSP1 (X.Y.Z-A.B.C) is bound to a specific Kotlin compiler version; KSP2 (e.g. 2.3.7) is
    // decoupled and supports Kotlin language version 2.0+. Default to KSP1 for the default
    // Kotlin 1.8.20, and switch to KSP2 when the matrix sets a Kotlin 2.x version.
    val KSP = System.getenv("VERSION_KOTLIN")?.let { if (it.startsWith("2.")) "2.3.7" else null }
        ?: "2.1.0-1.0.29"
}

object LibsVersion {
    const val SDK_VERSION = 35
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
