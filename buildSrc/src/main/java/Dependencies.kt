object BuildPluginsVersion {
    val AGP = System.getenv("VERSION_AGP") ?: "4.1.3"
    const val DOKKA = "1.4.32"
    const val KOTLIN = "1.4.32"
    const val KTLINT = "10.0.0"
    const val MAVEN_PUBLISH = "0.15.1"
}

object LibsVersion {
    const val JUNIT = "4.13.2"
}

object Libs {
    val AGP = "com.android.tools.build:gradle:${BuildPluginsVersion.AGP}"
    val JUNIT = "junit:junit:${LibsVersion.JUNIT}"
}
