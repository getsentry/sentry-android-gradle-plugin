plugins {
    alias(libs.plugins.androidApplication) version BuildPluginsVersion.AGP
    id("io.sentry.android.gradle")
}

android {
    compileSdk = LibsVersion.SDK_VERSION
    defaultConfig {
        minSdk = LibsVersion.MIN_SDK_VERSION
        targetSdk = LibsVersion.SDK_VERSION
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    namespace = "com.example.sampleapp"
}

sentry {
    autoUploadProguardMapping.set(CI.canAutoUpload())

    telemetryDsn.set(CI.SENTRY_SDKS_DSN)
    tracingInstrumentation {
        enabled.set(false)
    }
}
