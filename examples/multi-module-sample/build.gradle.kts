import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin)
  id("io.sentry.jvm.gradle")
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_11
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}

sentry {
  debug.set(true)
  telemetry.set(false)
  includeSourceContext.set(CI.canAutoUpload())
  additionalSourceDirsForSourceContext.set(setOf("testsrc"))
}
