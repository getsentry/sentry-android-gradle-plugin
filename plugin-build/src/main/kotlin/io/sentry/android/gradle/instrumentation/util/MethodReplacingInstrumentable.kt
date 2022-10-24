package io.sentry.android.gradle.instrumentation.util

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

open class MethodReplacingInstrumentable(private val replacements: List<MethodReplacement>) :
    ClassInstrumentable {

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor {
        return MethodReplacingClassVisitor(apiVersion, originalVisitor, replacements)
    }

    override fun isInstrumentable(data: ClassContext): Boolean = !data.isSentryClass()

    data class MethodReplacement(
        val originalOpcode: Int,
        val originalOwner: String?,
        val originalName: String?,
        val originalDescriptor: String?,

        val newOwner: String?,
        val newName: String?
    )
}

class MethodReplacingClassVisitor(
    private val apiVersion: Int,
    originalVisitor: ClassVisitor,
    replacements: List<MethodReplacingInstrumentable.MethodReplacement>
) : ClassVisitor(apiVersion, originalVisitor) {

    private val replacementsMap =
        replacements.associateBy {
            fingerprintInstruction(
                it.originalOpcode,
                it.originalOwner,
                it.originalName,
                it.originalDescriptor,
            )
        }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor {
        return object : MethodVisitor(
            apiVersion,
            super.visitMethod(access, name, descriptor, signature, exceptions)
        ) {
            override fun visitMethodInsn(
                opcode: Int,
                owner: String?,
                name: String?,
                descriptor: String?,
                isInterface: Boolean
            ) {
                val replacement =
                    replacementsMap[
                        fingerprintInstruction(
                            opcode,
                            owner,
                            name,
                            descriptor,
                        )
                    ]

                val newName = replacement?.newName ?: name
                val newOwner = replacement?.newOwner ?: owner
                super.visitMethodInsn(
                    opcode,
                    newOwner,
                    newName,
                    descriptor,
                    isInterface
                )
            }
        }
    }

    private fun fingerprintInstruction(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
    ) = "$opcode-$owner.$name-$descriptor"
}
