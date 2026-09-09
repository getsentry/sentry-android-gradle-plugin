package io.sentry.android.gradle.extensions

import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SentryVariantConfigResolverTest {

  private fun extension(): SentryPluginExtension {
    val project = ProjectBuilder.builder().build()
    return project.extensions.create("sentry", SentryPluginExtension::class.java)
  }

  @Test
  fun `falls back to global defaults when no overrides are set`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.runtimeOptimizations.enabled.set(false)

    val config =
      SentryVariantConfigResolver.resolve(extension = extension, variantName = "demoRelease")

    assertThat(config.tracingInstrumentationEnabled).isTrue()
    assertThat(config.runtimeOptimizationsEnabled).isFalse()
  }

  @Test
  fun `variant override wins over global default`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.runtimeOptimizations.enabled.set(true)
    extension.variants.create("demoDebug") {
      it.tracingInstrumentation.enabled.set(false)
      it.runtimeOptimizations.enabled.set(false)
    }

    val config =
      SentryVariantConfigResolver.resolve(extension = extension, variantName = "demoDebug")

    assertThat(config.tracingInstrumentationEnabled).isFalse()
    assertThat(config.runtimeOptimizationsEnabled).isFalse()
  }

  @Test
  fun `variant override only affects matching variant`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.variants.create("demoDebug") { it.tracingInstrumentation.enabled.set(false) }

    val debug =
      SentryVariantConfigResolver.resolve(extension = extension, variantName = "demoDebug")
    val release =
      SentryVariantConfigResolver.resolve(extension = extension, variantName = "demoRelease")

    assertThat(debug.tracingInstrumentationEnabled).isFalse()
    assertThat(release.tracingInstrumentationEnabled).isTrue()
  }

  @Test
  fun `unset override properties inherit independently`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.runtimeOptimizations.enabled.set(true)
    extension.variants.create("debug") {
      // only disable tracing; runtime optimizations should inherit global true
      it.tracingInstrumentation.enabled.set(false)
    }

    val config = SentryVariantConfigResolver.resolve(extension = extension, variantName = "debug")

    assertThat(config.tracingInstrumentationEnabled).isFalse()
    assertThat(config.runtimeOptimizationsEnabled).isTrue()
  }
}
