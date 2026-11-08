package io.sentry.android.gradle.extensions

import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class TracingInstrumentationExtensionTest {

  private val defaultFeatures =
    setOf(
      InstrumentationFeature.DATABASE,
      InstrumentationFeature.FILE_IO,
      InstrumentationFeature.OKHTTP,
      InstrumentationFeature.COMPOSE,
    )

  @Test
  fun `navigation3 enabled is false by default`() {
    val extension = createExtension()

    assertThat(extension.navigation3.enabled.get()).isFalse()
  }

  @Test
  fun `navigation3 action enables Navigation 3 instrumentation`() {
    val extension = createExtension()

    extension.navigation3 { it.enabled.set(true) }

    assertThat(extension.navigation3.enabled.get()).isTrue()
  }

  @Test
  fun `enabling Navigation 3 instrumentation keeps the default instrumentation features`() {
    val extension = createExtension()

    extension.navigation3 { it.enabled.set(true) }

    assertThat(extension.features.get()).containsExactlyElementsIn(defaultFeatures)
  }

  private fun createExtension(): TracingInstrumentationExtension =
    ProjectBuilder.builder()
      .build()
      .objects
      .newInstance(TracingInstrumentationExtension::class.java)
}
