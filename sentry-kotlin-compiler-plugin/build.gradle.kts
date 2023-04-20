plugins {
    kotlin("jvm") version "1.8.20"
    kotlin("kapt")
    id("com.vanniktech.maven.publish") version "0.24.0"
    id("org.jetbrains.compose") version "1.4.0"
}

repositories {
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

mavenPublishing {
    // needs to match plugin-build/src/main/kotlin/io/sentry/SentryKotlinCompilerGradlePlugin.kt
    coordinates("io.sentry", "sentry-kotlin-compiler-plugin", "1.0.0-mah-dev-026")
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")

    // TODO we actually only want to depend on some runtime classes for testing our plugin
    implementation(compose.desktop.currentOs)
}

plugins.withId("com.vanniktech.maven.publish.base") {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = "mavenTestRepo"
                url = file("${rootProject.buildDir}/mavenTestRepo").toURI()
            }
        }
    }
}
