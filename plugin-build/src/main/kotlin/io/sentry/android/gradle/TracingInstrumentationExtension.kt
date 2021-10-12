package io.sentry.android.gradle

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class TracingInstrumentationExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Enable the tracing instrumentation.
     * Does bytecode manipulation for 'androidx.sqlite' and 'androidx.room' libraries.
     * It starts and finishes a Span within any CRUD operation performed by sqlite or room.
     */
    val enabled: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    /**
     * Enabled debug output of the plugin. Useful when there are issues with code instrumentation,
     * shows the modified bytecode.
     * Defaults to false.
     */
    val debug: Property<Boolean> = objects.property(Boolean::class.java).convention(
        false
    )

    /**
     * Forces dependencies instrumentation, even if they were already instrumented.
     * Useful when there are issues with code instrumentation, e.g. the dependencies are
     * partially instrumented.
     * Defaults to false.
     */
    val forceInstrumentDependencies: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)
}
