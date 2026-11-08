package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class Navigation3Extension @Inject constructor(objects: ObjectFactory) {
  /**
   * Experimental. Enables or disables Navigation 3 auto-instrumentation. When enabled, the SDK
   * records navigation transactions, breadcrumbs, and screen names for every `NavDisplay(backStack
   * = ...)` call, without calling `SentryNavEffect` manually. Back stack entries are named after
   * their kotlinx.serialization serial names; entries that aren't `@Serializable` are recorded as
   * `/unknown`. Arguments are not recorded.
   *
   * This feature uses bytecode manipulation: it modifies `NavDisplay` inside
   * `androidx.navigation3:navigation3-ui` so that it observes its back stack. `NavDisplay(entries =
   * ...)` and `NavDisplay(sceneState = ...)` aren't covered; call `SentryNavEffect` manually for
   * those.
   *
   * Requires `io.sentry:sentry-android-navigation3` and `androidx.navigation3:navigation3-ui` 1.0.0
   * or newer. To turn it off again, set this to false and release a new build; there's no runtime
   * switch.
   *
   * Defaults to false while the feature is experimental.
   */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
}
