package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class AppStartExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Enables or disables the app start instrumentation feature.
     * When enabled, all ContentProviders and the Application onCreate methods will be instrumented.
     *
     * Defaults to true.
     */
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
