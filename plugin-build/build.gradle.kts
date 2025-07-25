import io.sentry.android.gradle.internal.ASMifyTask
import io.sentry.android.gradle.internal.BootstrapAndroidSdk
import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.groovyGradlePlugin) version BuildPluginsVersion.GROOVY_REDISTRIBUTED
  alias(libs.plugins.kotlin)
  id("distribution")
  alias(libs.plugins.dokka)
  id("java-gradle-plugin")
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.spotless)
  // we need this plugin in order to include .aar dependencies into a pure java project, which the
  // gradle plugin is
  id("io.sentry.android.gradle.aar2jar")
  alias(libs.plugins.buildConfig)
}

BootstrapAndroidSdk.locateAndroidSdk(project, extra)

val androidSdkPath: String? by extra
val testImplementationAar by configurations.getting // this converts .aar into .jar dependencies

val fixtureClasspath: Configuration by configurations.creating

dependencies {
  compileOnly(libs.gradleApi)
  compileOnly(Libs.AGP)
  compileOnly(libs.proguard)

  implementation(libs.asm)
  implementation(libs.asmCommons)

  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${KotlinCompilerVersion.VERSION}")

  implementation(libs.sentry)

  testImplementation(gradleTestKit())
  testImplementation(kotlin("test"))
  testImplementation(Libs.AGP)
  testImplementation(libs.proguard)
  testImplementation(libs.junit)
  testImplementation(libs.mockitoKotlin)
  testImplementation(libs.truth)

  testImplementation(libs.asm)
  testImplementation(libs.asmCommons)

  // we need these dependencies for tests, because the bytecode verifier also analyzes superclasses
  testImplementationAar(libs.sqlite)
  testImplementationAar(libs.sqliteFramework)
  testRuntimeOnly(files(androidSdkPath))
  testImplementationAar(libs.sentryAndroid)
  testImplementationAar(libs.sentryAndroidOkhttp)
  testImplementationAar(libs.sentryOkhttp)

  // Needed to read contents from APK/Source Bundles
  testImplementation(libs.arscLib)
  testImplementation(libs.zip4j)
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

// We need to compile Groovy first and let Kotlin depend on it.
// See https://docs.gradle.org/6.1-rc-1/release-notes.html#compilation-order
tasks.withType<GroovyCompile>().configureEach {
  sourceCompatibility = JavaVersion.VERSION_11.toString()
  targetCompatibility = JavaVersion.VERSION_11.toString()

  // we don't need the groovy compile task for compatibility source sets
  val ignoreTask = name.contains("agp", ignoreCase = true)
  isEnabled = !ignoreTask
  if (!ignoreTask) {
    classpath = sourceSets["main"].compileClasspath
  }
}

tasks.withType<KotlinCompile>().configureEach {
  if (!name.contains("agp", ignoreCase = true)) {
    libraries.from.addAll(files(sourceSets["main"].groovy.classesDirectory))
  }

  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_11.toString()
    languageVersion = "1.8"
    apiVersion = "1.8"
  }
}

// Append any extra dependencies to the test fixtures via a custom configuration classpath. This
// allows us to apply additional plugins in a fixture while still leveraging dependency resolution
// and de-duplication semantics.
tasks.named("pluginUnderTestMetadata").configure {
  (this as PluginUnderTestMetadata).pluginClasspath.from(fixtureClasspath)
}

tasks.withType<Test>().named("test").configure {
  maxParallelForks = 2

  // Cap JVM args per test
  minHeapSize = "128m"
  maxHeapSize = "1g"

  filter { excludeTestsMatching("io.sentry.android.gradle.integration.*") }
}

tasks.register<Test>("integrationTest").configure {
  group = "verification"
  description = "Runs the integration tests"
  // for some reason Gradle > 8.10 doesn't pick up the pluginUnderTestMetadata classpath, so we
  // need to add it manually
  classpath += layout.files(project.layout.buildDirectory.dir("pluginUnderTestMetadata"))

  maxParallelForks = 2

  // Cap JVM args per test
  minHeapSize = "128m"
  maxHeapSize = "1g"

  jvmArgs =
    listOf(
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.io=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
      "--add-opens=java.base/java.net=ALL-UNNAMED",
      "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
    )

  filter { includeTestsMatching("io.sentry.android.gradle.integration.*") }
  dependsOn(tasks.withType<PluginUnderTestMetadata>())
}

gradlePlugin {
  plugins {
    register("sentryPlugin") {
      id = "io.sentry.android.gradle"
      implementationClass = "io.sentry.android.gradle.SentryPlugin"
    }
    register("kotlinCompilerPlugin") {
      id = "io.sentry.kotlin.compiler.gradle"
      implementationClass = "io.sentry.kotlin.gradle.SentryKotlinCompilerGradlePlugin"
    }
    register("sentryJvmPlugin") {
      id = "io.sentry.jvm.gradle"
      implementationClass = "io.sentry.jvm.gradle.SentryJvmPlugin"
    }
  }
}

spotless {
  kotlin {
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
    target("**/*.kt")
  }
  kotlinGradle {
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
    target("**/*.kts")
  }
}

val sep = File.separator

distributions {
  main {
    contents {
      from("build${sep}libs")
      from("build${sep}publications${sep}pluginMaven")
    }
  }
  create("sentryPluginMarker") {
    contents { from("build${sep}publications${sep}sentryPluginPluginMarkerMaven") }
  }
  create("sentryKotlinCompilerPluginMarker") {
    contents { from("build${sep}publications${sep}kotlinCompilerPluginPluginMarkerMaven") }
  }
  create("sentryJvmPluginMarker") {
    contents { from("build${sep}publications${sep}sentryJvmPluginPluginMarkerMaven") }
  }
}

tasks.named("distZip") {
  dependsOn("publishToMavenLocal")
  onlyIf { inputs.sourceFiles.isEmpty.not().also { require(it) { "No distribution to zip." } } }
}

tasks.named("distTar").configure {
  dependsOn(
    "dokkaJavadocJar",
    "jar",
    "sourcesJar",
    "generateMetadataFileForPluginMavenPublication",
    "generatePomFileForPluginMavenPublication",
  )
}

tasks.named("sentryJvmPluginMarkerDistTar").configure {
  dependsOn(
    "generatePomFileForSentryJvmPluginPluginMarkerMavenPublication",
    "generatePomFileForKotlinCompilerPluginPluginMarkerMavenPublication",
  )
}

tasks.named("sentryJvmPluginMarkerDistZip").configure {
  dependsOn("generatePomFileForSentryJvmPluginPluginMarkerMavenPublication")
}

tasks.named("dokkaHtml").configure { dependsOn("compileGroovy") }

tasks.named("sentryKotlinCompilerPluginMarkerDistTar").configure {
  dependsOn("generatePomFileForKotlinCompilerPluginPluginMarkerMavenPublication")
}

tasks.named("sentryKotlinCompilerPluginMarkerDistZip").configure {
  dependsOn("generatePomFileForKotlinCompilerPluginPluginMarkerMavenPublication")
}

tasks.named("sentryPluginMarkerDistTar").configure {
  dependsOn("generatePomFileForSentryPluginPluginMarkerMavenPublication")
}

tasks.named("sentryPluginMarkerDistZip").configure {
  dependsOn("generatePomFileForSentryPluginPluginMarkerMavenPublication")
}

tasks.withType<Test>().configureEach {
  testLogging {
    events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED)
    showStandardStreams = true
  }
}

val downloadSentryCLI =
  tasks.register<Exec>("downloadSentryCLI") {
    onlyIf { shouldDownloadSentryCli() }
    doFirst { logger.lifecycle("Downloading Sentry CLI...") }
    executable("sh")
    workingDir("../plugin-build")
    args("-c", "./download-sentry-cli.sh")
  }

tasks.named("processResources").configure { dependsOn(downloadSentryCLI) }

/**
 * Checks whether the sentry-cli.properties matches the copy in `./src/main/resources/bin/`. If it
 * doesn't, the CLI should be re-downloaded.
 */
fun shouldDownloadSentryCli(): Boolean {
  val cliDir: Array<File> = File("$projectDir/src/main/resources/bin/").listFiles() ?: emptyArray()
  val expectedSpec = File("$projectDir/sentry-cli.properties")
  val actualSpec = File("$projectDir/src/main/resources/bin/sentry-cli.properties")
  return when {
    cliDir.size <= 2 -> {
      logger.lifecycle("Sentry CLI is missing")
      true
    }

    !actualSpec.exists() -> {
      logger.lifecycle("Sentry CLI version specification is missing")
      true
    }

    expectedSpec.readText() != actualSpec.readText() -> {
      logger.lifecycle("Downloaded Sentry CLI version specification doesn't match")
      true
    }

    else -> false
  }
}

buildConfig {
  useKotlinOutput()
  packageName("io.sentry")
  className("BuildConfig")

  buildConfigField("String", "Version", provider { "\"${project.version}\"" })
  buildConfigField("String", "SdkVersion", provider { "\"${project.property("sdk_version")}\"" })
  buildConfigField("String", "AgpVersion", provider { "\"${BuildPluginsVersion.AGP}\"" })
  buildConfigField(
    "String",
    "CliVersion",
    provider {
      "\"${Properties().apply {
          load(FileInputStream(File("$projectDir/sentry-cli.properties")))
      }.getProperty("version")}\""
    },
  )
}

tasks.register<ASMifyTask>("asmify")

tasks.named("check").configure { dependsOn(tasks.named("validatePlugins")) }

tasks.withType<ValidatePlugins>().configureEach {
  failOnWarning.set(true)
  enableStricterValidation.set(true)
}
