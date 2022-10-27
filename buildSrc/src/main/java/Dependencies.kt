import org.gradle.util.VersionNumber

object BuildPluginsVersion {
    val AGP = System.getenv("VERSION_AGP") ?: "7.3.0"
    const val DOKKA = "1.7.20"
    const val KOTLIN = "1.7.20"
    const val AAR_2_JAR = "0.6"
    const val KTLINT = "10.2.1"
    // do not upgrade to 0.18.0, it does not generate the pom-default.xml and module.json under
    // build/publications/maven
    const val MAVEN_PUBLISH = "0.17.0"
    const val PROGUARD = "7.1.0"
	// proguard does not support AGP 8 yet
    fun isProguardApplicable(): Boolean = VersionNumber.parse(AGP).major < 8
}

object LibsVersion {
    const val SDK_VERSION = 33
    const val MIN_SDK_VERSION = 21

    const val JUNIT = "4.13.2"
    const val ASM = "7.0" // compatibility matrix -> https://developer.android.com/reference/tools/gradle-api/7.1/com/android/build/api/instrumentation/InstrumentationContext#apiversion
    const val SQLITE = "2.1.0"
    const val SENTRY = "5.5.0"
}

object Libs {
    val AGP = "com.android.tools.build:gradle:${BuildPluginsVersion.AGP}"
    const val JUNIT = "junit:junit:${LibsVersion.JUNIT}"
    const val PROGUARD = "com.guardsquare:proguard-gradle:${BuildPluginsVersion.PROGUARD}"

    // bytecode instrumentation
    const val ASM = "org.ow2.asm:asm-util:${LibsVersion.ASM}"
    const val ASM_COMMONS = "org.ow2.asm:asm-commons:${LibsVersion.ASM}"
    const val SQLITE = "androidx.sqlite:sqlite:${LibsVersion.SQLITE}"
    const val SQLITE_FRAMEWORK = "androidx.sqlite:sqlite-framework:${LibsVersion.SQLITE}"
    const val SENTRY_ANDROID = "io.sentry:sentry-android:${LibsVersion.SENTRY}"

    // test
    val MOCKITO_KOTLIN = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
}

object CI {
    fun canAutoUpload(): Boolean {
        return System.getenv("AUTO_UPLOAD").toBoolean() &&
                !System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty()
    }
}

object Samples {
    object AndroidX {
        const val recyclerView = "androidx.recyclerview:recyclerview:1.2.0"
        const val lifecycle = "androidx.lifecycle:lifecycle-runtime-ktx:2.2.0"
        const val appcompat = "androidx.appcompat:appcompat:1.2.0"

        const val composeRuntime = "androidx.compose.runtime:runtime:1.2.1"
        const val composeNavigation = "androidx.navigation:navigation-compose:2.5.2"
        const val composeActivity = "androidx.activity:activity-compose:1.4.0"
        const val composeFoundation = "androidx.compose.foundation:foundation:1.2.1"
        const val composeFoundationLayout = "androidx.compose.foundation:foundation-layout:1.2.1"
    }

    object Coroutines {
        private const val version = "1.5.2"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
    }

    object Room {
        private const val version = "2.4.3"
        const val runtime = "androidx.room:room-runtime:${version}"
        const val ktx = "androidx.room:room-ktx:${version}"
        const val compiler = "androidx.room:room-compiler:${version}"
        const val rxjava = "androidx.room:room-rxjava2:${version}"
    }

    object Retrofit {
        private const val version = "2.9.0"
        const val retrofit = "com.squareup.retrofit2:retrofit:${version}"
        const val retrofitGson = "com.squareup.retrofit2:converter-gson:${version}"
    }

    object Timber {
        private const val version = "5.0.1"
        const val timber = "com.jakewharton.timber:timber:${version}"
    }

    object Fragment {
        private const val version = "1.3.5"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:${version}"
    }
}
