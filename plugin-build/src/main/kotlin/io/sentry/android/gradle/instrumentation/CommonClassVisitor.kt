package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.instrumentation.util.FileLogTextifier
import java.io.File
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.util.TraceMethodVisitor

@Suppress("UnstableApiUsage")
class CommonClassVisitor(
    apiVersion: Int,
    classVisitor: ClassVisitor,
    className: String,
    private val methodInstrumentables: List<MethodInstrumentable>,
    private val parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
) : ClassVisitor(apiVersion, classVisitor) {

    private lateinit var log: File

    init {
        // to avoid file creation in case the debug mode is not set
        if (parameters.debug.get()) {

            // create log dir.
            val logDir = parameters.tmpDir.get()
            logDir.mkdirs()

            // delete and recreate file
            log = File(parameters.tmpDir.get(), "$className-instrumentation.log")
            if (log.exists()) {
                log.delete()
            }
            log.createNewFile()
        }
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        val methodContext = MethodContext(access, name, descriptor, signature, exceptions?.toList())
        val instrumentable = methodInstrumentables.find { it.isInstrumentable(methodContext) }

        if (parameters.debug.get() && instrumentable != null) {
            mv = TraceMethodVisitor(mv, FileLogTextifier(api, log, name, descriptor))
        }

        return instrumentable?.getVisitor(methodContext, api, mv, parameters) ?: mv
    }
}
