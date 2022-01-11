package io.sentry.android.gradle.instrumentation.util

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class AnalyzingVisitor(
    apiVersion: Int,
    private val nextVisitor: (List<MethodNode>) -> ClassVisitor
) : ClassNode(apiVersion) {

    override fun visitEnd() {
        super.visitEnd()
        accept(nextVisitor(methods))
    }
}
