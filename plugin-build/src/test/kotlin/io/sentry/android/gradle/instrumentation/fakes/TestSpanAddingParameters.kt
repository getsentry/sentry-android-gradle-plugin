package io.sentry.android.gradle.instrumentation.fakes

import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.services.SentryModulesService
import java.io.File
import org.gradle.api.internal.provider.DefaultProperty
import org.gradle.api.internal.provider.PropertyHost
import org.gradle.api.provider.Property

class TestSpanAddingParameters(
    private val debugOutput: Boolean = true,
    private val inMemoryDir: File
) : SpanAddingClassVisitorFactory.SpanAddingParameters {

    override val invalidate: Property<Long>
        get() = DefaultProperty(PropertyHost.NO_OP, Long::class.java).convention(0L)

    override val debug: Property<Boolean>
        get() = DefaultProperty(PropertyHost.NO_OP, Boolean::class.javaObjectType)
            .convention(debugOutput)

    override val sentryModulesService: Property<SentryModulesService>
        get() = TODO()

    override val tmpDir: Property<File>
        get() = DefaultProperty<File>(PropertyHost.NO_OP, File::class.java).convention(inMemoryDir)

    override var _instrumentable: ClassInstrumentable? = null
}
