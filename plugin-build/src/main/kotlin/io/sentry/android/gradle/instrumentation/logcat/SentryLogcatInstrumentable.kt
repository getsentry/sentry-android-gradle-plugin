package io.sentry.android.gradle.instrumentation.logcat

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.util.isSentryClass
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper

class SentryLogcatInstrumentable : ClassInstrumentable {
    companion object {
        private val mapping = mapOf(
            "android/util/Log" to "io/sentry/android/core/SentryLogcatAdapter",
        )
    }

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor =
        ClassRemapper(originalVisitor, SimpleRemapper(mapping))

    override fun isInstrumentable(data: ClassContext) =
        !data.isSentryClass() && !data.currentClassData.className.contains("SentryLogcatAdapter\$Companion")
}
