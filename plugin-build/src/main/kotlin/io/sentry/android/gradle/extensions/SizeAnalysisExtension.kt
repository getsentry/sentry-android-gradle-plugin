package io.sentry.android.gradle.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class SizeAnalysisExtension @Inject constructor(objects : ObjectFactory) {
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
}
