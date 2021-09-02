object BuildPluginsVersion {
    val AGP = System.getenv("VERSION_AGP") ?: "7.0.1"
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

object Samples {
    object Sentry {
        private const val version = "5.1.2"
        const val android = "io.sentry:sentry-android:$version"
    }

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
    }
}
