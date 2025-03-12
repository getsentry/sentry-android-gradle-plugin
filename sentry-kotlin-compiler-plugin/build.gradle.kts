plugins {
    alias(libs.plugins.kotlin) version "1.9.24"
    alias(libs.plugins.kapt) version "1.9.24"
    id("distribution")
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.spotless)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}


spotless {
    kotlin {
        ktfmt(libs.versions.ktfmt.get()).googleStyle()
        targetExclude("**/generated/**")
    }
    kotlinGradle {
        ktfmt(libs.versions.ktfmt.get()).googleStyle()
        targetExclude("**/generated/**")
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
    compileOnly(libs.kotlinCompilerEmbeddable)

    kapt(libs.autoService)
    compileOnly(libs.autoServiceAnnotatons)

    testImplementation(libs.kotlinJunit)
    testImplementation(libs.kotlinCompilerEmbeddable)
    testImplementation(libs.kotlinCompileTesting)
    testImplementation(libs.composeDesktop)
}

kapt {
    correctErrorTypes = true
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
