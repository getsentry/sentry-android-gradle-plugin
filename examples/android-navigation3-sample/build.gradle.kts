// Exercises Navigation 3 auto-instrumentation (tracingInstrumentation.navigation3). See README.md.
plugins {
  alias(libs.plugins.androidApplication) version BuildPluginsVersion.AGP
  alias(libs.plugins.kotlinAndroid) version BuildPluginsVersion.KOTLIN
  id("org.jetbrains.kotlin.plugin.serialization") version BuildPluginsVersion.KOTLIN
  id("io.sentry.android.gradle")
}

// The root build already puts the Compose compiler plugin on the classpath.
apply(plugin = "org.jetbrains.kotlin.plugin.compose")

android {
  // Navigation 3 1.2 requires compileSdk 36 and minSdk 24.
  compileSdk = 36
  defaultConfig {
    minSdk = 24
    targetSdk = 36
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

  signingConfigs {
    getByName("debug") {
      storeFile = file("debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  namespace = "io.sentry.samples.navigation3"

  buildFeatures { compose = true }
}

kotlin { jvmToolchain(17) }

dependencies {
  implementation(libs.sample.androidx.navigation3.ui)
  implementation(libs.sample.androidx.navigation3.runtime)
  implementation(libs.sample.androidx.navigation3.activityCompose)
  implementation(libs.sample.androidx.navigation3.composeFoundation)

  // Auto-instrumentation only applies with a sentry-android-navigation3 version that contains its
  // entry point (see SentryVersions.VERSION_NAVIGATION3). To try an unreleased SDK, publish
  // sentry-java to Maven Local (./gradlew publishToMavenLocal -PversionName=<version>) and build
  // with -PuseLocalSentrySdk -PsentryNavigation3Version=<version>.
  val navigation3SdkVersion =
    providers.gradleProperty("sentryNavigation3Version").orElse(libs.versions.sentry).get()
  implementation("io.sentry:sentry-android-navigation3:$navigation3SdkVersion")
}

sentry {
  debug.set(true)
  autoUploadProguardMapping.set(CI.canAutoUpload())

  org.set("sentry-sdks")
  projectName.set("sentry-android")
  telemetryDsn.set(CI.SENTRY_SDKS_DSN)

  tracingInstrumentation {
    forceInstrumentDependencies.set(true)
    navigation3 { enabled.set(true) }
  }
}
