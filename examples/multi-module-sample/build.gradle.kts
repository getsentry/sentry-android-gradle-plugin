import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin)
  id("io.sentry.jvm.gradle")
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = JavaVersion.VERSION_1_8.toString()
  }
}

sentry {
  debug.set(true)
  telemetry.set(false)
  includeSourceContext.set(CI.canAutoUpload())
  additionalSourceDirsForSourceContext.set(setOf("testsrc"))
}
