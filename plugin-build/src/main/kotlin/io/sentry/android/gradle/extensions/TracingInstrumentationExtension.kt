package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

open class TracingInstrumentationExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Enable the tracing instrumentation.
     * Does bytecode manipulation for specified [features].
     * Defaults to true.
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

    /**
     * Specifies a set of [InstrumentationFeature] features that are eligible for bytecode
     * manipulation.
     * Defaults to all available features of [InstrumentationFeature].
     */
    val features: SetProperty<InstrumentationFeature> =
        objects.setProperty(InstrumentationFeature::class.java).convention(
            setOf(
                InstrumentationFeature.DATABASE,
                InstrumentationFeature.FILE_IO,
                InstrumentationFeature.OKHTTP,
                InstrumentationFeature.COMPOSE,
            )
        )
}

enum class InstrumentationFeature(val integrationName: String) {
    /**
     * When enabled the SDK will create spans for any CRUD operation performed by 'androidx.sqlite'
     * and 'androidx.room'. This feature uses bytecode manipulation.
     */
    DATABASE("Database Instrumentation"),

    /**
     * When enabled the SDK will create spans for [java.io.FileInputStream],
     * [java.io.FileOutputStream], [java.io.FileReader], [java.io.FileWriter].
     * This feature uses bytecode manipulation and replaces the above
     * mentioned classes with Sentry-specific implementations.
     */
    FILE_IO("File_IO Instrumentation"),

    /**
     * When enabled the SDK will create spans for outgoing network requests and attach
     * sentry-trace-header for distributed tracing.
     * This feature uses bytecode manipulation and attaches SentryOkHttpInterceptor to all OkHttp
     * clients in the project.
     */
    OKHTTP("OkHttp Instrumentation"),

    /**
     * When enabled the SDK will create breadcrumbs when navigating
     * using [androidx.navigation.NavController].
     * This feature uses bytecode manipulation and adds an OnDestinationChangedListener to all
     * navigation controllers used in Jetpack Compose.
     */
    COMPOSE("Compose Instrumentation")
}
