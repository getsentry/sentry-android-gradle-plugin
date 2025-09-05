import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin)
  id("io.sentry.jvm.gradle")
}

tasks.named<KotlinCompile>("compileKotlin") {
  compilerOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget.set(JvmTarget.JVM_1_8)
  }
}

sentry {
  debug.set(true)
  telemetry.set(false)
  includeSourceContext.set(CI.canAutoUpload())
  additionalSourceDirsForSourceContext.set(setOf("testsrc"))
}
