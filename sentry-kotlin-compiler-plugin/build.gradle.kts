import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin) version "2.1.20"
  alias(libs.plugins.kapt) version "2.1.20"
  id("distribution")
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.spotless)
}

val kotlin19: SourceSet by sourceSets.creating
val kotlin21: SourceSet by sourceSets.creating

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

val publish =
  extensions.getByType(com.vanniktech.maven.publish.MavenPublishPluginExtension::class.java)

// signing is done when uploading files to MC
// via gpg:sign-and-deploy-file (release.kts)
publish.releaseSigningEnabled = false

tasks.named("distZip") {
  dependsOn("publishToMavenLocal")
  onlyIf { inputs.sourceFiles.isEmpty.not().also { require(it) { "No distribution to zip." } } }
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

  kotlin19.compileOnlyConfigurationName("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.24")
  kotlin21.compileOnlyConfigurationName("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.20")

  compileOnly(kotlin19.output)
  compileOnly(kotlin21.output)
}

kapt { correctErrorTypes = true }

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

tasks.withType<Jar> {
  from(kotlin19.output)
  from(kotlin21.output)
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
    languageVersion = "1.9"
    apiVersion = "1.9"
  }
}
