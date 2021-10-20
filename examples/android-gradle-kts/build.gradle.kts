plugins {
    id("com.android.application")
    id("io.sentry.android.gradle")
}

android {
    compileSdk = 30
    defaultConfig {
        minSdk = 21
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

sentry {
    autoUpload.set(System.getenv("AUTO_UPLOAD")?.toBoolean() ?: false)

    tracingInstrumentation {
        enabled.set(false)
    }
}
