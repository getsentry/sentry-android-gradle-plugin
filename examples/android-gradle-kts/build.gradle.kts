plugins {
    id("com.android.application")
    id("io.sentry.android.gradle")
}

android {
    compileSdkVersion(30)
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            minifyEnabled(true)
        }
    }
}

sentry {
    autoUpload = false
}
