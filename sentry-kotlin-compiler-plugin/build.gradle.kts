plugins {
    kotlin("jvm") version "1.8.20"
    kotlin("kapt") version "1.8.20"
    id("distribution")
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

val sep = File.separator
distributions {
    main {
        contents {
            from("build${sep}libs")
            from("build${sep}publications${sep}maven")
        }
    }
}

val publish = extensions.getByType(
    com.vanniktech.maven.publish.MavenPublishPluginExtension::class.java
)
// signing is done when uploading files to MC
// via gpg:sign-and-deploy-file (release.kts)
publish.releaseSigningEnabled = false

tasks.named("distZip") {
    dependsOn("publishToMavenLocal")
    onlyIf {
        inputs.sourceFiles.isEmpty.not().also {
            require(it) { "No distribution to zip." }
        }
    }
}

repositories {
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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
                url = file("${rootProject.projectDir}/../build/mavenTestRepo").toURI()
            }
        }
    }
}
