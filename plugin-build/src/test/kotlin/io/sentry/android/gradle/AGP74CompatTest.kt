package io.sentry.android.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AGP74CompatTest {

  @Test
  fun `reads application optimization from analytics delegate`() {
    val variant = AnalyticsVariant(RawVariant(OptimizationCreationConfig(true)))

    assertThat(variant.applicationOptimizationEnabled()).isTrue()
  }

  @Test
  fun `reads application optimization from raw variant`() {
    val variant = RawVariant(OptimizationCreationConfig(false))

    assertThat(variant.applicationOptimizationEnabled()).isFalse()
  }

  class AnalyticsVariant(private val delegate: Any) {
    fun getDelegate(): Any = delegate
  }

  class RawVariant(private val optimizationCreationConfig: Any) {
    fun getOptimizationCreationConfig(): Any = optimizationCreationConfig
  }

  class OptimizationCreationConfig(private val applicationOptimizationEnabled: Boolean) {
    fun getApplicationOptimizationEnabled(): Boolean = applicationOptimizationEnabled
  }
}
