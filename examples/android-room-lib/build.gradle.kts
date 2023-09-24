plugins {
    id("com.android.library")
    kotlin("android")
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

tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask>().configureEach {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
}

dependencies {
    implementation(Samples.Coroutines.core)
    implementation(Samples.Coroutines.android)

    implementation(Samples.Room.runtime)
    implementation(Samples.Room.ktx)

    // this is here for test purposes, to ensure that transitive dependencies are also recognized
    // by our auto-installation
    api(Samples.Retrofit.retrofit)
    api(Samples.Retrofit.retrofitGson)
}
