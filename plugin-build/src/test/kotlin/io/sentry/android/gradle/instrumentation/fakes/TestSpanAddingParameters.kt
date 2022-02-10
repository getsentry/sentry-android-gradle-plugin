package io.sentry.android.gradle.instrumentation.fakes

import io.sentry.android.gradle.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.services.SentrySdkStateHolder
import java.io.File
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.provider.DefaultProperty
import org.gradle.api.internal.provider.DefaultSetProperty
import org.gradle.api.internal.provider.PropertyHost
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

class TestSpanAddingParameters(
    private val debugOutput: Boolean = true,
    private val inMemoryDir: File
) : SpanAddingClassVisitorFactory.SpanAddingParameters {

    override val invalidate: Property<Long>
        get() = DefaultProperty(PropertyHost.NO_OP, Long::class.java).convention(0L)

    override val debug: Property<Boolean>
        get() = DefaultProperty(PropertyHost.NO_OP, Boolean::class.javaObjectType)
            .convention(debugOutput)

    override val features: SetProperty<InstrumentationFeature>
        get() = DefaultSetProperty(PropertyHost.NO_OP, InstrumentationFeature::class.java)
            .convention(setOf(InstrumentationFeature.FILE_IO, InstrumentationFeature.DATABASE))

    override val sdkStateHolder: Property<SentrySdkStateHolder>
        get() = TODO()

    override val tmpDir: Property<File>
        get() = DefaultProperty<File>(PropertyHost.NO_OP, File::class.java).convention(inMemoryDir)

    override var _instrumentables: List<ClassInstrumentable>? = listOf()
}
