plugins {
    kotlin("jvm") version "1.8.20"
    kotlin("kapt") version "1.8.20"
    id("com.vanniktech.maven.publish") version "0.17.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

ktlint {
    debug.set(false)
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

val publish = extensions.getByType(
    com.vanniktech.maven.publish.MavenPublishPluginExtension::class.java
)
// signing is done when uploading files to MC
// via gpg:sign-and-deploy-file (release.kts)
publish.releaseSigningEnabled = false

repositories {
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
    testImplementation("org.jetbrains.compose.desktop:desktop:1.4.0")
}

plugins.withId("com.vanniktech.maven.publish.base") {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = "mavenTestRepo"
                url = file("${rootProject.projectDir}/../build/mavenTestRepo").toURI()
            }
        }
    }
}
