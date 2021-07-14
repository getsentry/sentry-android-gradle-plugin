import com.vanniktech.maven.publish.MavenPublishPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version BuildPluginsVersion.KOTLIN
    id("distribution")
    id("org.jetbrains.dokka") version BuildPluginsVersion.DOKKA
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish") version BuildPluginsVersion.MAVEN_PUBLISH apply false
    id("org.jlleitschuh.gradle.ktlint") version BuildPluginsVersion.KTLINT
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(Libs.AGP)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation(Libs.AGP)
    testImplementation(Libs.JUNIT)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
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

// We conditionally apply the maven.publish plugin only if we're on Gradle 6.6.0+
// as such plugin is not compatible with lower versions of Gradle and will break
// the CI matrix.
if (gradle.gradleVersion >= "6.6.0") {
    apply {
        plugin("com.vanniktech.maven.publish")
    }

    val publish = extensions.getByType(MavenPublishPluginExtension::class.java)
    publish.releaseSigningEnabled = BuildUtils.shouldSignArtifacts()
}

val sep = File.separator

configure<DistributionContainer> {
    this.getByName("main").contents {
        from("build${sep}libs")
        from("build${sep}publications${sep}maven")
        from("build${sep}publications${sep}sentryPluginPluginMarkerMaven")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.named("distZip").configure {
    this.dependsOn("publishToMavenLocal")
    this.doLast {
        val distributionFilePath = "${this.project.buildDir}${sep}distributions" +
            "${sep}${this.project.name}-${this.project.version}.zip"
        val file = File(distributionFilePath)
        if (!file.exists()) {
            throw IllegalStateException(
                "Distribution file: $distributionFilePath does not exist"
            )
        }
    }
}
