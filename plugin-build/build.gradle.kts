import com.vanniktech.maven.publish.MavenPublishPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version BuildPluginsVersion.KOTLIN
    id("org.jetbrains.dokka") version BuildPluginsVersion.DOKKA
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish") version BuildPluginsVersion.MAVEN_PUBLISH apply false
    id("org.jlleitschuh.gradle.ktlint") version BuildPluginsVersion.KTLINT
}

repositories {
    jcenter()
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
    }
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
    extensions.getByType(MavenPublishPluginExtension::class.java).releaseSigningEnabled =
        BuildUtils.shouldSignArtifacts()
}
