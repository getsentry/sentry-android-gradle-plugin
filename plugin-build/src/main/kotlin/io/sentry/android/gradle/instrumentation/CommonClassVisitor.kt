package io.sentry.android.gradle.instrumentation

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class CommonClassVisitor(
    private val methodInstrumentables: List<Instrumentable<MethodVisitor>>,
    apiVersion: Int,
    classVisitor: ClassVisitor
) : ClassVisitor(apiVersion, classVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        val instrumentable = methodInstrumentables.find { it.fqName == name }
        return instrumentable?.getVisitor(api, mv, descriptor) ?: mv
    }
}
