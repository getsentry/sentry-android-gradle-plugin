package io.sentry.android.gradle.instrumentation

import java.io.File
import org.gradle.api.internal.provider.DefaultProperty
import org.gradle.api.internal.provider.PropertyHost
import org.gradle.api.provider.Property

class TestSpanAddingParameters(
    private val inMemoryDir: File
) : SpanAddingClassVisitorFactory.SpanAddingParameters {

    override val invalidate: Property<Long>
        get() = DefaultProperty(PropertyHost.NO_OP, Long::class.java).convention(0L)

    override val debug: Property<Boolean>
        get() = DefaultProperty(PropertyHost.NO_OP, Boolean::class.javaObjectType).convention(true)

    override val tmpDir: Property<File>
        get() = DefaultProperty<File>(PropertyHost.NO_OP, File::class.java).convention(inMemoryDir)
}
