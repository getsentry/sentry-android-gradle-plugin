package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
open class SnapshotPreviewsExtension @Inject constructor(objects: ObjectFactory) {

  val generateTests: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

  val includePrivatePreviews: Property<Boolean> =
    objects.property(Boolean::class.java).convention(true)

  val packageTrees: ListProperty<String> =
    objects.listProperty(String::class.java).convention(emptyList())

  val theme: Property<String> = objects.property(String::class.java)

  /**
   * Number of test classes the previews are split across. Gradle hands out work to test forks one
   * class at a time, so a single generated class runs in one fork no matter how high
   * `maxParallelForks` is set. Splitting the previews into several classes lets the forks share the
   * work.
   *
   * This only helps if the snapshot test task also has `maxParallelForks` set above 1. Every fork
   * starts its own layoutlib, so memory use grows with the number of forks that run at once.
   *
   * Use a fixed value. A value computed from the machine, such as its CPU count, changes the
   * generated source from one machine to the next and causes build cache misses.
   *
   * Defaults to 1.
   */
  @ApiStatus.Experimental
  val testClassCount: Property<Int> = objects.property(Int::class.java).convention(1)
}
