plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("io.sentry.android.gradle")
}

// useful for local debugging of the androidx.sqlite lib
// make sure to checkout the lib from https://github.com/androidx/androidx/tree/androidx-main/sqlite/sqlite-framework
//configurations.all {
//    resolutionStrategy.dependencySubstitution {
//        substitute(module("androidx.sqlite:sqlite-framework")).using(project(":sqlite-framework"))
//    }
//}

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

object Deps {

    object Sentry {
        private const val version = "5.1.2"
        const val android = "io.sentry:sentry-android:$version"
    }

    object Kotlin {
        private const val version = "1.5.21"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
    }

    object AndroidX {
        const val recyclerView = "androidx.recyclerview:recyclerview:1.2.0"
        const val lifecycle = "androidx.lifecycle:lifecycle-runtime-ktx:2.2.0"
        const val appcompat = "androidx.appcompat:appcompat:1.2.0"
    }

    object Coroutines {
        private const val version = "1.5.1"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
    }

    object Room {
        private const val version = "2.3.0"
        const val runtime = "androidx.room:room-runtime:${version}"
        const val ktx = "androidx.room:room-ktx:${version}"
        const val compiler = "androidx.room:room-compiler:${version}"
    }
}

dependencies {
    implementation(Deps.Sentry.android)

    implementation(Deps.Kotlin.stdlib)

    implementation(Deps.AndroidX.recyclerView)
    implementation(Deps.AndroidX.lifecycle)
    implementation(Deps.AndroidX.appcompat)

    implementation(Deps.Coroutines.core)
    implementation(Deps.Coroutines.android)

    implementation(Deps.Room.runtime)
    implementation(Deps.Room.ktx)

    kapt(Deps.Room.compiler)
}

sentry {
    forceInstrumentDependencies.set(true)
}
