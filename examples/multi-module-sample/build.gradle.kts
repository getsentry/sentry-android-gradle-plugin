import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin)
  id("io.sentry.jvm.gradle")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
    jvmTarget.set(JvmTarget.JVM_1_8)
  }
}

sentry {
  debug.set(true)
  telemetry.set(false)
  includeSourceContext.set(CI.canAutoUpload())
  additionalSourceDirsForSourceContext.set(setOf("testsrc"))
}
