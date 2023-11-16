// ktlint-disable max-line-length
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("io.sentry.jvm.gradle")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

sentry {
    debug.set(true)
    telemetry.set(false)
    includeSourceContext.set(true)
    additionalSourceDirsForSourceContext.set(setOf("testsrc"))
}
