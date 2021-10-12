plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("io.sentry.android.gradle")
}

// useful for local debugging of the androidx.sqlite lib
// make sure to checkout the lib from https://github.com/androidx/androidx/tree/androidx-main/sqlite/sqlite-framework
// configurations.all {
//    resolutionStrategy.dependencySubstitution {
//        substitute(module("androidx.sqlite:sqlite-framework")).using(project(":sqlite-framework"))
//    }
// }

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
            signingConfig = signingConfigs.getByName("debug")
        }
    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//    kotlinOptions {
//        jvmTarget = JavaVersion.VERSION_11.toString()
//    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
}

// useful, when we want to modify room-generated classes, and then compile them into .class files
// so room does not re-generate and overwrite our changes
// afterEvaluate {
//    tasks.getByName("kaptDebugKotlin").enabled = false
// }

dependencies {
    implementation(Libs.SENTRY_ANDROID)

    implementation(Samples.AndroidX.recyclerView)
    implementation(Samples.AndroidX.lifecycle)
    implementation(Samples.AndroidX.appcompat)

    implementation(Samples.Coroutines.core)
    implementation(Samples.Coroutines.android)

    implementation(Samples.Room.runtime)
    implementation(Samples.Room.ktx)
    implementation(Samples.Room.rxjava)

    kapt(Samples.Room.compiler)
}

sentry {
    autoUpload.set(false)

    tracingInstrumentation {
        forceInstrumentDependencies.set(true)
        debug.set(true)
    }
}
