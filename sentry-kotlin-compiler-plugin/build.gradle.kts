plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("kapt")
    id("com.vanniktech.maven.publish") version "0.24.0"
    id("org.jetbrains.compose") version "1.3.1"
}

repositories {
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

mavenPublishing {
    // needs to match plugin-build/src/main/kotlin/io/sentry/SentryKotlinCompilerGradlePlugin.kt
    coordinates("io.sentry", "sentry-kotlin-compiler-plugin", "1.0.0-SNAPSHOT")
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")

    implementation(compose.desktop.currentOs)
}

