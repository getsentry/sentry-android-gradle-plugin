@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.remap

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.util.isSentryClass
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper

class RemappingInstrumentable : ClassInstrumentable {
    companion object {
        private val mapping = mapOf(
            "java/io/FileReader" to "io/sentry/instrumentation/file/SentryFileReader",
            "java/io/FileWriter" to "io/sentry/instrumentation/file/SentryFileWriter"
        )
    }

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor =
        ClassRemapper(originalVisitor, SimpleRemapper(mapping))

    override fun isInstrumentable(data: ClassContext): Boolean = !data.isSentryClass()
}
