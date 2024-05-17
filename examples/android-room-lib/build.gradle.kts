plugins {
    alias(libs.plugins.androidLibrary) version BuildPluginsVersion.AGP
    alias(libs.plugins.kotlinAndroid)
}

android {
    compileSdk = LibsVersion.SDK_VERSION
    defaultConfig {
        minSdk = LibsVersion.MIN_SDK_VERSION
        targetSdk = LibsVersion.SDK_VERSION
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    namespace = "io.sentry.android.instrumentation.lib"
}

dependencies {
    implementation(libs.sample.coroutines.core)
    implementation(libs.sample.coroutines.android)

    implementation(libs.sample.room.runtime)
    implementation(libs.sample.room.ktx)

    // this is here for test purposes, to ensure that transitive dependencies are also recognized
    // by our auto-installation
    api(libs.sample.retrofit.retrofit)
    api(libs.sample.retrofit.retrofitGson)
}
