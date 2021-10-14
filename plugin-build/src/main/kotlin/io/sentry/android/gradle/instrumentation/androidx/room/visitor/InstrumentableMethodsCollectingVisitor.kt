package io.sentry.android.gradle.instrumentation.androidx.room.visitor

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class InstrumentableMethodsCollectingVisitor(
    private val apiVersion: Int,
    private val nextVisitorInitializer: (Set<MethodNode>) -> ClassVisitor
) : ClassNode(apiVersion) {

    private val methodsToInstrument = mutableSetOf<MethodNode>()

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methodNode = super.visitMethod(
            access,
            name,
            descriptor,
            signature,
            exceptions
        ) as MethodNode

        return object : MethodVisitor(apiVersion, methodNode) {

            override fun visitMethodInsn(
                opcode: Int,
                owner: String?,
                name: String?,
                descriptor: String?,
                isInterface: Boolean
            ) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                if (owner to name in lookup) {
                    methodsToInstrument.add(methodNode)
                }
            }
        }
    }

    override fun visitEnd() {
        super.visitEnd()
        val nextVisitor = nextVisitorInitializer(methodsToInstrument)
        accept(nextVisitor)
    }

    companion object {
        private val lookup = listOf(
            "androidx/room/RoomDatabase" to "beginTransaction",
            "androidx/room/util/DBUtil" to "query"
        )
    }
}
