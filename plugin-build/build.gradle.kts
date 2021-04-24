import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version BuildPluginsVersion.KOTLIN
    id("java-gradle-plugin")
    id("maven-publish")
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

java {
    withJavadocJar()
    withSourcesJar()
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

/* ktlint-disable max-line-length */
publishing {
    publications {
        this.withType(MavenPublication::class.java) {
            pom {
                name.set("Sentry Android Gradle Plugin")
                description.set("Sentry Android Gradle Plugin")
                url.set("https://github.com/getsentry/sentry-android-gradle-plugin")

                scm {
                    url.set("https://github.com/getsentry/sentry-android-gradle-plugin")
                    connection.set(
                        "scm:https://github.com/getsentry/sentry-android-gradle-plugin.git"
                    )
                    developerConnection.set(
                        "scm:git@github.com:getsentry/sentry-android-gradle-plugin.git"
                    )
                }

                licenses {
                    license {
                        name.set("MIT")
                        url.set(
                            "https://github.com/getsentry/sentry-android-gradle-plugin/blob/master/LICENSE"
                        )
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("getsentry")
                        name.set("Sentry Team and Contributors")
                        email.set("oss@sentry.io")
                    }
                }
            }
        }
    }
}
/* ktlint-enable max-line-length */
