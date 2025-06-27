import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin)
  id("io.sentry.jvm.gradle")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(11))
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
  jvmToolchain(11)
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}

sentry {
  debug.set(true)
  telemetry.set(false)
  includeSourceContext.set(CI.canAutoUpload())
  additionalSourceDirsForSourceContext.set(setOf("testsrc"))
}
