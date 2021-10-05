import com.vanniktech.maven.publish.MavenPublishPluginExtension
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.configurationcache.extensions.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
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
apply(from = "gradle/bootstrap-sdk.gradle.kts")

val androidSdkPath: String? by extra
val testImplementationAar by configurations.getting // this converts .aar into .jar dependencies

dependencies {
    compileOnly(gradleApi())
    compileOnly(Libs.AGP)

    implementation(Libs.ASM)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation(Libs.AGP)
    testImplementation(Libs.JUNIT)

    testImplementation(Libs.ASM)

    // we need these dependencies for tests, because the bytecode verifier also analyzes superclasses
    testImplementationAar(Libs.SQLITE)
    testImplementationAar(Libs.SQLITE_FRAMEWORK)
    testRuntimeOnly(files(androidSdkPath))

    testRuntimeOnly(
        files(
            serviceOf<ModuleRegistry>().getModule("gradle-tooling-api-builders")
                .classpath.asFiles.first()
        )
    )
}

tasks.withType<KotlinCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        languageVersion = "1.3"
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

// We conditionally apply the maven.publish plugin only if we're on Gradle 6.6.0+
// as such plugin is not compatible with lower versions of Gradle and will break
// the CI matrix.
if (gradle.gradleVersion >= "6.6.0") {
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
 * When bumping the Sentry CLI, you should update the `expected-checksums.sha` file
 * to match the `checksums.sha` file from `./src/main/resources/bin/`
 *
 * That's to retrigger a download of the cli upon a bump.
 */
fun shouldDownloadSentryCli(): Boolean {
    val cliDir: Array<File> = File(
        "$projectDir/src/main/resources/bin/"
    ).listFiles() ?: emptyArray()
    val expectedChecksums = File("$projectDir/expected-checksums.sha")
    val actualChecksums = File("$projectDir/src/main/resources/bin/checksums.sha")
    return when {
        cliDir.size <= 2 -> {
            logger.lifecycle("Sentry CLI is missing")
            true
        }
        !actualChecksums.exists() -> {
            logger.lifecycle("Sentry CLI Checksums is missing")
            true
        }
        expectedChecksums.readText() != actualChecksums.readText() -> {
            logger.lifecycle("Sentry CLI Checksums doesn't match")
            true
        }
        else -> false
    }
}
