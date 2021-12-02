package io.sentry.android.gradle.instrumentation.wrap.visitor

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.wrap.Replacement
import io.sentry.android.gradle.util.info
import io.sentry.android.gradle.util.warn
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

class WrappingVisitor(
    api: Int,
    originalVisitor: MethodVisitor,
    private val className: String,
    private val context: MethodContext,
    private val replacements: Map<Replacement, Replacement>
) : GeneratorAdapter(
    api,
    originalVisitor,
    context.access,
    context.name,
    context.descriptor
) {

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val methodSig = Replacement(owner, name, descriptor)
        val replacement = replacements[methodSig]
        when {
            opcode == Opcodes.INVOKEDYNAMIC -> {
                // we don't instrument invokedynamic, because it's just forwarding to a synthetic method
                // which will be instrumented thanks to condition below
                SentryPlugin.logger.warn { "INVOKEDYNAMIC skipped from instrumentation for $className.${context.name}" }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            }
            replacement != null -> {
                val isSuperCallInOverride = opcode == Opcodes.INVOKESPECIAL &&
                    owner != className &&
                    name == context.name &&
                    descriptor == context.descriptor

                if (isSuperCallInOverride) {
                    SentryPlugin.logger.info {
                        "$className skipped from instrumentation in overridden method $name.$descriptor"
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                } else {
                    SentryPlugin.logger.info {
                        "Wrapping $owner.$name with ${replacement.owner}.${replacement.name} in $className.${context.name}"
                    }
                    visitWrapping(replacement, opcode, owner, name, descriptor, isInterface)
                }
            }
            else -> super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }

    private fun GeneratorAdapter.visitWrapping(
        replacement: Replacement,
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        // create a new method to figure out the number of arguments
        val newMethod = Method(name, descriptor)

        // replicate arguments on stack, so we can later re-use them for our wrapping
        val locals = IntArray(newMethod.argumentTypes.size)
        for (i in locals.size - 1 downTo 0) {
            locals[i] = newLocal(newMethod.argumentTypes[i])
            storeLocal(locals[i])
        }

        // load arguments from stack for the original method call
        locals.forEach {
            loadLocal(it)
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

        // load arguments from stack for the wrapping method call
        locals.forEach {
            loadLocal(it)
        }
        // call wrapping (it's always a static method)
        super.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            replacement.owner,
            replacement.name,
            replacement.descriptor,
            false
        )
    }
}
