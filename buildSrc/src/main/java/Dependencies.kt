import BuildPluginsVersion.SPRING_BOOT
import org.gradle.util.VersionNumber

object BuildPluginsVersion {
    val AGP = System.getenv("VERSION_AGP") ?: "8.0.2"
    const val DOKKA = "1.8.10"
    const val KOTLIN = "1.9.20"
    const val AAR_2_JAR = "0.6"
    const val KTLINT = "10.2.1"
    const val SHADOW = "7.1.2"
    // do not upgrade to 0.18.0, it does not generate the pom-default.xml and module.json under
    // build/publications/maven
    const val MAVEN_PUBLISH = "0.17.0"
    const val PROGUARD = "7.1.0"
    const val GROOVY_REDISTRIBUTED = "1.2"
    const val BUILDCONFIG = "3.1.0"

    const val SPRING_BOOT = "2.7.4"
    const val SPRING_DEP_MANAGEMENT = "1.0.11.RELEASE"

	// proguard does not support AGP 8 yet
    fun isProguardApplicable(): Boolean = VersionNumber.parse(AGP).major < 8
}

object LibsVersion {
    const val SDK_VERSION = 34
    const val MIN_SDK_VERSION = 21

    const val JUNIT = "4.13.2"
    const val ASM = "7.0" // compatibility matrix -> https://developer.android.com/reference/tools/gradle-api/7.1/com/android/build/api/instrumentation/InstrumentationContext#apiversion
    const val SQLITE = "2.1.0"
    const val SENTRY = "6.31.0"
}

object Libs {
    fun agp(version: String) = "com.android.tools.build:gradle:$version"
    val AGP = "com.android.tools.build:gradle:${BuildPluginsVersion.AGP}"
    const val JUNIT = "junit:junit:${LibsVersion.JUNIT}"
    const val PROGUARD = "com.guardsquare:proguard-gradle:${BuildPluginsVersion.PROGUARD}"
    // this allows us to develop against a fixed version of Gradle, as opposed to depending on the
    // locally available version. kotlin-gradle-plugin follows the same approach.
    // More info: https://docs.nokee.dev/manual/gradle-plugin-development-plugin.html
    const val GRADLE_API = "dev.gradleplugins:gradle-api:7.6"

    // bytecode instrumentation
    const val ASM = "org.ow2.asm:asm-util:${LibsVersion.ASM}"
    const val ASM_COMMONS = "org.ow2.asm:asm-commons:${LibsVersion.ASM}"
    const val SQLITE = "androidx.sqlite:sqlite:${LibsVersion.SQLITE}"
    const val SQLITE_FRAMEWORK = "androidx.sqlite:sqlite-framework:${LibsVersion.SQLITE}"
    const val SENTRY = "io.sentry:sentry:${LibsVersion.SENTRY}"
    const val SENTRY_ANDROID = "io.sentry:sentry-android:${LibsVersion.SENTRY}"
    const val SENTRY_ANDROID_OKHTTP = "io.sentry:sentry-android-okhttp:${LibsVersion.SENTRY}"

    // test
    val MOCKITO_KOTLIN = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
    const val ARSC_LIB = "io.github.reandroid:ARSCLib:1.1.4"
    const val ZIP4J = "net.lingala.zip4j:zip4j:2.11.5"
}

object CI {
    const val SENTRY_SDKS_DSN = "https://dd1f82ad30a331bd7def2a0dce926c6e@o447951.ingest.sentry.io/4506031723446272"
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

        const val composeRuntime = "androidx.compose.runtime:runtime:1.1.1"
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
        private const val version = "2.6.0"
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

    object SpringBoot {
        val springBoot = "org.springframework.boot"
        val springDependencyManagement = "io.spring.dependency-management"
        val springBootStarter = "org.springframework.boot:spring-boot-starter:$SPRING_BOOT"
        val springBootStarterTest = "org.springframework.boot:spring-boot-starter-test:$SPRING_BOOT"
        val springBootStarterWeb = "org.springframework.boot:spring-boot-starter-web:$SPRING_BOOT"
        val springBootStarterWebflux = "org.springframework.boot:spring-boot-starter-webflux:$SPRING_BOOT"
        val springBootStarterAop = "org.springframework.boot:spring-boot-starter-aop:$SPRING_BOOT"
        val springBootStarterSecurity = "org.springframework.boot:spring-boot-starter-security:$SPRING_BOOT"
        val springBootStarterJdbc = "org.springframework.boot:spring-boot-starter-jdbc:$SPRING_BOOT"
        val hsqldb = "org.hsqldb:hsqldb:2.6.1"
        val aspectj = "org.aspectj:aspectjweaver"
        val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect"
        val kotlinStdLib = "stdlib-jdk8"
    }
}
