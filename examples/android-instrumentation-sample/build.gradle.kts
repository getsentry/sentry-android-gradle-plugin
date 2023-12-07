plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("io.sentry.android.gradle")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

// useful for local debugging of the androidx.sqlite lib
// make sure to checkout the lib from https://github.com/androidx/androidx/tree/androidx-main/sqlite/sqlite-framework
// configurations.all {
//    resolutionStrategy.dependencySubstitution {
//        substitute(module("androidx.sqlite:sqlite-framework")).using(project(":sqlite-framework"))
//    }
// }

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
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    flavorDimensions += "environment"
    // just a random product flavor for compatibility testing against AGP
    productFlavors {
        create("staging") {
            dimension = "environment"
            versionNameSuffix = "-staging"
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    namespace = "io.sentry.samples.instrumentation"

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

kotlin {
    jvmToolchain(11)
}

// useful, when we want to modify room-generated classes, and then compile them into .class files
// so room does not re-generate and overwrite our changes
// afterEvaluate {
//    tasks.getByName("kaptDebugKotlin").enabled = false
// }

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(Samples.AndroidX.recyclerView)
    implementation(Samples.AndroidX.lifecycle)
    implementation(Samples.AndroidX.appcompat)

    implementation(Samples.AndroidX.composeRuntime)
    implementation(Samples.AndroidX.composeActivity)
    implementation(Samples.AndroidX.composeFoundation)
    implementation(Samples.AndroidX.composeFoundationLayout)
    implementation(Samples.AndroidX.composeNavigation)

    implementation(Samples.Coroutines.core)
    implementation(Samples.Coroutines.android)

    implementation(Samples.Room.runtime)
    implementation(Samples.Room.ktx)
    implementation(Samples.Room.rxjava)

    implementation(Samples.Timber.timber)
    implementation(Samples.Fragment.fragmentKtx)
    implementation(project(":examples:android-room-lib"))

    ksp(Samples.Room.compiler)
}

sentry {
    debug.set(true)
    autoUploadProguardMapping.set(CI.canAutoUpload())

    includeSourceContext.set(true)
    autoUploadSourceContext.set(CI.canAutoUpload())
    additionalSourceDirsForSourceContext.set(setOf("src/custom/java"))

    org.set("sentry-sdks")
    projectName.set("sentry-android")
    telemetryDsn.set(CI.SENTRY_SDKS_DSN)

    tracingInstrumentation {
        forceInstrumentDependencies.set(true)
    }
}
