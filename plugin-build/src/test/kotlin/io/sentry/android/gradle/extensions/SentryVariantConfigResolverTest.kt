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
      SentryVariantConfigResolver.resolve(
        extension = extension,
        variantName = "demoRelease",
        buildType = "release",
        productFlavors = listOf("demo"),
      )

    assertThat(config.tracingInstrumentationEnabled).isTrue()
    assertThat(config.runtimeOptimizationsEnabled).isFalse()
  }

  @Test
  fun `build type override wins over global default`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.runtimeOptimizations.enabled.set(true)
    extension.buildTypes.create("debug") {
      it.tracingInstrumentation.enabled.set(false)
      it.runtimeOptimizations.enabled.set(false)
    }

    val config =
      SentryVariantConfigResolver.resolve(
        extension = extension,
        variantName = "demoDebug",
        buildType = "debug",
        productFlavors = listOf("demo"),
      )

    assertThat(config.tracingInstrumentationEnabled).isFalse()
    assertThat(config.runtimeOptimizationsEnabled).isFalse()
  }

  @Test
  fun `product flavor override is used when build type is unset`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.productFlavors.create("full") { it.tracingInstrumentation.enabled.set(false) }

    val config =
      SentryVariantConfigResolver.resolve(
        extension = extension,
        variantName = "fullRelease",
        buildType = "release",
        productFlavors = listOf("full"),
      )

    assertThat(config.tracingInstrumentationEnabled).isFalse()
  }

  @Test
  fun `earlier product flavor wins when multiple flavors set the same property`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.productFlavors.create("paid") { it.tracingInstrumentation.enabled.set(false) }
    extension.productFlavors.create("demo") { it.tracingInstrumentation.enabled.set(true) }

    val config =
      SentryVariantConfigResolver.resolve(
        extension = extension,
        variantName = "paidDemoRelease",
        buildType = "release",
        productFlavors = listOf("paid", "demo"),
      )

    assertThat(config.tracingInstrumentationEnabled).isFalse()
  }

  @Test
  fun `variant override wins over build type and flavor`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.runtimeOptimizations.enabled.set(true)
    extension.buildTypes.create("debug") {
      it.tracingInstrumentation.enabled.set(false)
      it.runtimeOptimizations.enabled.set(false)
    }
    extension.productFlavors.create("full") {
      it.tracingInstrumentation.enabled.set(false)
      it.runtimeOptimizations.enabled.set(false)
    }
    extension.variants.create("fullDebug") {
      it.tracingInstrumentation.enabled.set(true)
      it.runtimeOptimizations.enabled.set(true)
    }

    val config =
      SentryVariantConfigResolver.resolve(
        extension = extension,
        variantName = "fullDebug",
        buildType = "debug",
        productFlavors = listOf("full"),
      )

    assertThat(config.tracingInstrumentationEnabled).isTrue()
    assertThat(config.runtimeOptimizationsEnabled).isTrue()
  }

  @Test
  fun `build type override only affects matching build type`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.buildTypes.create("debug") { it.tracingInstrumentation.enabled.set(false) }

    val debug =
      SentryVariantConfigResolver.resolve(
        extension = extension,
        variantName = "demoDebug",
        buildType = "debug",
        productFlavors = listOf("demo"),
      )
    val release =
      SentryVariantConfigResolver.resolve(
        extension = extension,
        variantName = "demoRelease",
        buildType = "release",
        productFlavors = listOf("demo"),
      )

    assertThat(debug.tracingInstrumentationEnabled).isFalse()
    assertThat(release.tracingInstrumentationEnabled).isTrue()
  }

  @Test
  fun `unset override properties inherit independently`() {
    val extension = extension()
    extension.tracingInstrumentation.enabled.set(true)
    extension.runtimeOptimizations.enabled.set(true)
    extension.buildTypes.create("debug") {
      // only disable tracing; runtime optimizations should inherit global true
      it.tracingInstrumentation.enabled.set(false)
    }

    val config =
      SentryVariantConfigResolver.resolve(
        extension = extension,
        variantName = "debug",
        buildType = "debug",
        productFlavors = emptyList(),
      )

    assertThat(config.tracingInstrumentationEnabled).isFalse()
    assertThat(config.runtimeOptimizationsEnabled).isTrue()
  }
}
