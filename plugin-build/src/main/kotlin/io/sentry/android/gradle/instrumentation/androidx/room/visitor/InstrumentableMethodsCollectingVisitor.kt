package io.sentry.android.gradle.instrumentation.androidx.room.visitor

import io.sentry.android.gradle.instrumentation.androidx.room.RoomMethodType
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.slf4j.LoggerFactory

class InstrumentableMethodsCollectingVisitor(
    private val apiVersion: Int,
    private val nextVisitorInitializer: (List<Pair<MethodNode, RoomMethodType>>) -> ClassVisitor
) : ClassNode(apiVersion) {

    private val methodsToInstrument = mutableMapOf<MethodNode, RoomMethodType>()
    private val logger by lazy { LoggerFactory.getLogger(this::class.java) }

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
                val pair = owner to name
                if (pair in lookup) {
                    var type: RoomMethodType? = if (pair == lookup.first()) {
                        RoomMethodType.TRANSACTION
                    } else {
                        RoomMethodType.QUERY
                    }

                    // if this methodNode has already been added to the instrumentable list
                    // this means that either it's a SELECT query wrapped into a transaction
                    // or some unknown to us usecase for instrumentation and we rather skip it
                    if (methodNode in methodsToInstrument) {
                        /* ktlint-disable max-line-length */
                        val prevType = methodsToInstrument[methodNode]
                        type = when {
                            prevType == RoomMethodType.QUERY &&
                                type == RoomMethodType.TRANSACTION ->
                                RoomMethodType.QUERY_WITH_TRANSACTION
                            prevType == RoomMethodType.TRANSACTION &&
                                type == RoomMethodType.QUERY ->
                                RoomMethodType.QUERY_WITH_TRANSACTION
                            prevType == RoomMethodType.QUERY_WITH_TRANSACTION ->
                                RoomMethodType.QUERY_WITH_TRANSACTION
                            else -> {
                                logger.warn(
                                    "Unable to identify RoomMethodType, skipping $name from instrumentation"
                                )
                                null
                            }
                        }
                        /* ktlint-enable max-line-length */
                    }

                    if (type != null) {
                        methodsToInstrument[methodNode] = type
                    } else {
                        methodsToInstrument.remove(methodNode)
                    }
                }
            }
        }
    }

    override fun visitEnd() {
        super.visitEnd()
        val nextVisitor = nextVisitorInitializer(methodsToInstrument.toList())
        accept(nextVisitor)
    }

    companion object {
        private val lookup = listOf(
            "androidx/room/RoomDatabase" to "beginTransaction",
            "androidx/room/util/DBUtil" to "query"
        )
    }
}
