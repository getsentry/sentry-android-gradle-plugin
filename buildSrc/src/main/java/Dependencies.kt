object BuildPluginsVersion {
    val AGP = System.getenv("VERSION_AGP") ?: "7.0.3"
    const val DOKKA = "1.4.32"
    const val KOTLIN = "1.4.32"
    const val AAR_2_JAR = "0.6"
    const val KTLINT = "10.2.0"
    const val MAVEN_PUBLISH = "0.15.1"
}

object LibsVersion {
    const val JUNIT = "4.13.2"
    const val ASM = "9.2"
    const val SQLITE = "2.1.0"
    const val SENTRY = "5.1.2"
}

object Libs {
    val AGP = "com.android.tools.build:gradle:${BuildPluginsVersion.AGP}"
    const val JUNIT = "junit:junit:${LibsVersion.JUNIT}"

    // bytecode instrumentation
    const val ASM = "org.ow2.asm:asm-util:${LibsVersion.ASM}"
    const val ASM_COMMONS = "org.ow2.asm:asm-commons:${LibsVersion.ASM}"
    const val SQLITE = "androidx.sqlite:sqlite:${LibsVersion.SQLITE}"
    const val SQLITE_FRAMEWORK = "androidx.sqlite:sqlite-framework:${LibsVersion.SQLITE}"
    const val SENTRY_ANDROID = "io.sentry:sentry-android:${LibsVersion.SENTRY}"
}

object Samples {
    object AndroidX {
        const val recyclerView = "androidx.recyclerview:recyclerview:1.2.0"
        const val lifecycle = "androidx.lifecycle:lifecycle-runtime-ktx:2.2.0"
        const val appcompat = "androidx.appcompat:appcompat:1.2.0"
    }

    object Coroutines {
        private const val version = "1.5.1"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
    }

    object Room {
        private const val version = "2.3.0"
        const val runtime = "androidx.room:room-runtime:${version}"
        const val ktx = "androidx.room:room-ktx:${version}"
        const val compiler = "androidx.room:room-compiler:${version}"
        const val rxjava = "androidx.room:room-rxjava2:${version}"
    }
}
