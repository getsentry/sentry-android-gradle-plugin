import com.vanniktech.maven.publish.MavenPublishPluginExtension
import io.sentry.android.gradle.internal.ASMifyTask
import io.sentry.android.gradle.internal.BootstrapAndroidSdk
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.configurationcache.extensions.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("dev.gradleplugins.groovy-gradle-plugin") version BuildPluginsVersion.GROOVY_REDISTRIBUTED
    kotlin("jvm") version BuildPluginsVersion.KOTLIN
    id("distribution")
    id("org.jetbrains.dokka") version BuildPluginsVersion.DOKKA
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish") version BuildPluginsVersion.MAVEN_PUBLISH apply false
    id("org.jlleitschuh.gradle.ktlint") version BuildPluginsVersion.KTLINT
    // we need this plugin in order to include .aar dependencies into a pure java project, which the gradle plugin is
    id("com.stepango.aar2jar") version BuildPluginsVersion.AAR_2_JAR
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

BootstrapAndroidSdk.locateAndroidSdk(project, extra)

val androidSdkPath: String? by extra
val testImplementationAar by configurations.getting // this converts .aar into .jar dependencies

dependencies {
    compileOnly(Libs.GRADLE_API)
    compileOnly(Libs.AGP)
    compileOnly(Libs.PROGUARD)

    compileOnly(Libs.ASM)
    compileOnly(Libs.ASM_COMMONS)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation(Libs.AGP)
    testImplementation(Libs.PROGUARD)
    testImplementation(Libs.JUNIT)
    testImplementation(Libs.MOCKITO_KOTLIN)

    testImplementation(Libs.ASM)
    testImplementation(Libs.ASM_COMMONS)

    // we need these dependencies for tests, because the bytecode verifier also analyzes superclasses
    testImplementationAar(Libs.SQLITE)
    testImplementationAar(Libs.SQLITE_FRAMEWORK)
    testRuntimeOnly(files(androidSdkPath))
    testRuntimeOnly(Libs.SENTRY_ANDROID)

    testRuntimeOnly(
        files(
            serviceOf<ModuleRegistry>().getModule("gradle-tooling-api-builders")
                .classpath.asFiles.first()
        )
    )
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// We need to compile Groovy first and let Kotlin depend on it.
// See https://docs.gradle.org/6.1-rc-1/release-notes.html#compilation-order
tasks.withType<GroovyCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
    classpath = sourceSets["main"].compileClasspath
}

tasks.withType<KotlinCompile>().configureEach {
    classpath += files(sourceSets["main"].groovy.classesDirectory)

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xjvm-default=enable")
        languageVersion = "1.4"
        apiVersion = "1.4"
    }
}

tasks.withType<Test>().configureEach {
    maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
}

gradlePlugin {
    plugins {
        register("sentryPlugin") {
            id = "io.sentry.android.gradle"
            implementationClass = "io.sentry.android.gradle.SentryPlugin"
        }
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
    create("sentryPluginMarker") {
        contents {
            from("build${sep}publications${sep}sentryPluginPluginMarkerMaven")
        }
    }
}

apply {
    plugin("com.vanniktech.maven.publish")
}

val publish = extensions.getByType(MavenPublishPluginExtension::class.java)
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

tasks.withType<Test> {
    testLogging {
        events = setOf(
            TestLogEvent.SKIPPED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED
        )
        showStandardStreams = true
    }
}

val downloadSentryCLI = tasks.register<Exec>("downloadSentryCLI") {
    onlyIf {
        shouldDownloadSentryCli()
    }
    doFirst {
        logger.lifecycle("Downloading Sentry CLI...")
    }
    executable("sh")
    workingDir("../plugin-build")
    args("-c", "./download-sentry-cli.sh")
}

tasks.named("processResources").configure {
    dependsOn(downloadSentryCLI)
}

/**
 * Checks whether the sentry-cli.properties matches the copy in `./src/main/resources/bin/`.
 * If it doesn't, the CLI should be re-downloaded.
 */
fun shouldDownloadSentryCli(): Boolean {
    val cliDir: Array<File> = File(
        "$projectDir/src/main/resources/bin/"
    ).listFiles() ?: emptyArray()
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

tasks.register<ASMifyTask>("asmify")
