import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    `maven-publish`
    `java-gradle-plugin`
    id("com.bmuschko.nexus") version "2.3.1"
}

buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath(files())
    }
}

repositories {
    google()
    mavenCentral()
    jcenter()
    mavenLocal()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

publishing {
    publications {
        create("sentry-gradle-plugin", MavenPublication::class.java) {
            artifact(tasks.jar)
            groupId = "io.sentry"
            artifactId = "sentry-android-gradle-plugin"
            version = project.version.toString()

            pom {
                name.set("Sentry Android Gradle Plugin")
                description.set("Sentry Android Gradle Plugin")
                url.set("https://github.com/getsentry/sentry-android-gradle-plugin")

                scm {
                    url.set("https://github.com/getsentry/sentry-android-gradle-plugin")
                    connection.set("scm:https://github.com/getsentry/sentry-android-gradle-plugin.git")
                    developerConnection.set("scm:git@github.com:getsentry/sentry-android-gradle-plugin.git")
                }

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/getsentry/sentry-android-gradle-plugin/blob/master/LICENSE")
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

    repositories {
        maven {
            setUrl(buildDir.resolve("repo"))
        }
    }
}

val funcTestSourceSet = sourceSets.create("funcTest") {
    java.srcDir(file("src/funcTest/kotlin"))
    resources.srcDir(file("src/funcTest/resources"))
    compileClasspath += sourceSets.getByName("main").output
    runtimeClasspath += sourceSets.getByName("main").output
}

configurations.named("funcTestImplementation") {
    extendsFrom(configurations.getByName("implementation"))
}

configurations.named("funcTestRuntimeOnly") {
    extendsFrom(configurations.getByName("runtimeOnly"))
}

val funcTest = tasks.register("funcTest", Test::class.java) {
    testClassesDirs += files(funcTestSourceSet.output.classesDirs)
    classpath += files(funcTestSourceSet.runtimeClasspath)
    testLogging.exceptionFormat = TestExceptionFormat.FULL
}

tasks.check {
    dependsOn(funcTest)
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:4.1.0")

    add("funcTestImplementation", gradleTestKit())
    add("funcTestImplementation", kotlin("test"))
    add("funcTestImplementation", kotlin("test-junit"))
    add("funcTestImplementation", "org.jetbrains:annotations:19.0.0")
}

gradlePlugin {
    testSourceSets(funcTestSourceSet)
    plugins {
        register("sentry-gradle-plugin") {
            id = "io.sentry.android.gradle"
            implementationClass = "io.sentry.android.gradle.SentryPlugin"
        }
    }
}