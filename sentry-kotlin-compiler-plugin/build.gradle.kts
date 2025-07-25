import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  alias(libs.plugins.kotlin) version "2.1.0"
  alias(libs.plugins.kapt) version "2.1.0"
  id("distribution")
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.spotless)
}

val kotlin1920: SourceSet by sourceSets.creating
val kotlin2120: SourceSet by sourceSets.creating
val kotlin2200: SourceSet by sourceSets.creating

spotless {
  kotlin {
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
    target("**/*.kt")
  }
  kotlinGradle {
    target("**/*.kts")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
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

tasks.named("distZip") {
  dependsOn("publishToMavenLocal")
  onlyIf { inputs.sourceFiles.isEmpty.not().also { require(it) { "No distribution to zip." } } }
}

dependencies {
  compileOnly(libs.kotlinCompilerEmbeddable)

  kapt(libs.autoService)
  compileOnly(libs.autoServiceAnnotatons)

  testImplementation(libs.kotlinJunit)
  testImplementation(libs.kotlinCompilerEmbeddable)
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(libs.composeDesktop)
  testImplementation(kotlin1920.output)
  testImplementation(kotlin2120.output)
  testImplementation(kotlin2200.output)

  kotlin1920.compileOnlyConfigurationName("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.24")
  kotlin2120.compileOnlyConfigurationName("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.20")
  kotlin2200.compileOnlyConfigurationName("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.0")

  compileOnly(kotlin1920.output)
  compileOnly(kotlin2120.output)
  compileOnly(kotlin2200.output)
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

tasks.withType<Jar>().configureEach {
  from(kotlin1920.output)
  from(kotlin2120.output)
  from(kotlin2200.output)
}

// see
// https://youtrack.jetbrains.com/issue/KTIJ-24311/task-current-target-is-17-and-kaptGenerateStubsProductionDebugKotlin-task-current-target-is-1.8-jvm-target-compatibility-should
kotlin {
  jvmToolchain(11)
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
    languageVersion.set(KotlinVersion.KOTLIN_1_9)
    apiVersion.set(KotlinVersion.KOTLIN_1_9)
  }
}
