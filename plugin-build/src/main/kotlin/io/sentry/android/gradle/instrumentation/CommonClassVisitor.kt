package io.sentry.android.gradle.instrumentation

import io.sentry.android.gradle.instrumentation.util.FileLogTextifier
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.File

class CommonClassVisitor(
    apiVersion: Int,
    classVisitor: ClassVisitor,
    className: String,
    private val methodInstrumentables: List<Instrumentable<MethodVisitor>>,
    private val parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
) : ClassVisitor(apiVersion, classVisitor) {

    private lateinit var log: File

    init {
        // to avoid file creation in case the debug mode is not set
        if (parameters.debug.get()) {
            log = File(parameters.tmpDir.get().asFile, "$className-instrumentation.log")
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
        val instrumentable = methodInstrumentables.find { it.fqName == name }

        if (parameters.debug.get() && instrumentable != null) {
            mv = TraceMethodVisitor(mv, FileLogTextifier(log, name, descriptor))
        }

        return instrumentable?.getVisitor(api, mv, descriptor, parameters) ?: mv
    }
}
