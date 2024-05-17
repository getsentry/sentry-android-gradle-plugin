plugins {
    alias(libs.plugins.androidApplication) version BuildPluginsVersion.AGP
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kapt)
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
        kotlinCompilerExtensionVersion = "1.4.6"
    }

    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    jvmToolchain(11)
}

// useful, when we want to modify room-generated classes, and then compile them into .class files
// so room does not re-generate and overwrite our changes
// afterEvaluate {
//    tasks.getByName("kaptDebugKotlin").enabled = false
// }

dependencies {
    implementation(libs.sample.androidx.recyclerView)
    implementation(libs.sample.androidx.lifecycle)
    implementation(libs.sample.androidx.appcompat)

    implementation(libs.sample.androidx.composeRuntime)
    implementation(libs.sample.androidx.composeActivity)
    implementation(libs.sample.androidx.composeFoundation)
    implementation(libs.sample.androidx.composeFoundationLayout)
    implementation(libs.sample.androidx.composeNavigation)

    implementation(libs.sample.coroutines.core)
    implementation(libs.sample.coroutines.android)

    implementation(libs.sample.room.runtime)
    implementation(libs.sample.room.ktx)
    implementation(libs.sample.room.rxjava)

    implementation(libs.sample.timber.timber)
    implementation(project(":examples:android-room-lib"))
    implementation(libs.sample.fragment.fragmentKtx)

    kapt(libs.sample.room.compiler)
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
