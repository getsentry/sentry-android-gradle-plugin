package io.sentry.snapshots.runtime

import androidx.annotation.FloatRange

/**
 * Configures how Sentry compares this preview snapshot against the baseline.
 *
 * Apply alongside `@Preview` on a composable to override the global diff threshold on a
 * per-snapshot basis.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SentrySnapshot(
  /**
   * Minimum pixel-difference percentage required to report this snapshot as changed. Differences
   * below this value are treated as unchanged.
   *
   * Range: `0.0`..`1.0`. Example: `0.01f` reports a change only when at least 1% of pixels differ.
   * Defaults to `0f` (report any difference).
   */
  @FloatRange(from = 0.0, to = 1.0) val diffThreshold: Float = 0f
)
