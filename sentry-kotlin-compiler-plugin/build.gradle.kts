plugins {
    kotlin("jvm")
    kotlin("kapt")
    // can't use BuildPluginsVersion.MAVEN_PUBLISH here, as coordinates is not available in this version
    id("com.vanniktech.maven.publish") version "0.24.0"
}

repositories {
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

mavenPublishing {
    // needs to match plugin-build/src/main/kotlin/io/sentry/SentryKotlinCompilerGradlePlugin.kt
    coordinates("io.sentry", "sentry-kotlin-compiler-plugin", "${rootProject.version}")
}

dependencies {
    compileOnly(Libs.KOTLIN_COMPILE_EMBEDDABLE)

    kapt(Libs.AUTO_SERVICE)
    compileOnly(Libs.AUTO_SERVICE_ANNOTATIONS)

    testImplementation(kotlin("test-junit"))
    testImplementation(Libs.KOTLIN_COMPILE_EMBEDDABLE)
    testImplementation(Libs.KOTLIN_COMPILE_TESTING)
    testImplementation(Libs.COMPOSE_DESKTOP_RUNTIME)
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
