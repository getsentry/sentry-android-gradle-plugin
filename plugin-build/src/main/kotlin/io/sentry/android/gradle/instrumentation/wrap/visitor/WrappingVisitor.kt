@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle.instrumentation.wrap.visitor

import com.android.build.api.instrumentation.ClassData
import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.wrap.Replacement
import io.sentry.android.gradle.util.info
import java.util.LinkedList
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.slf4j.Logger

class WrappingVisitor(
    api: Int,
    originalVisitor: MethodVisitor,
    private val firstPassVisitor: MethodNode,
    private val classContext: ClassData,
    private val context: MethodContext,
    private val replacements: Map<Replacement, Replacement>,
    private val logger: Logger = SentryPlugin.logger
) : GeneratorAdapter(
    api,
    originalVisitor,
    context.access,
    context.name,
    context.descriptor
) {

    private val targetTypes = replacements.keys
        .toSet()
        .map { it.owner }

    private val newInsnsForTargets by lazy {
        // filter all NEW insns for the target types that we are going to wrap
        val insns = firstPassVisitor.instructions
            .toArray()
            .filter { it is TypeInsnNode && it.opcode == Opcodes.NEW && it.desc in targetTypes }
        LinkedList(insns)
    }
    private val className = classContext.className.replace('.', '/')

    private var newWithoutDupInsn = false

    override fun visitTypeInsn(opcode: Int, type: String?) {
        super.visitTypeInsn(opcode, type)
        if (opcode == Opcodes.NEW && type in targetTypes) {
            val nextInsn = newInsnsForTargets.poll()?.next
            // in case the next insn after NEW is not a DUP, we inject our own DUP and flip a flag
            // to later store our wrapping instance with the same index as the original instance
            if (nextInsn != null && (nextInsn as? InsnNode)?.opcode != Opcodes.DUP) {
                dup()
                newWithoutDupInsn = true
            }
        }
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val methodSig = Replacement(owner, name, descriptor)
        val replacement = if (methodSig in replacements) {
            replacements[methodSig]
        } else {
            // try to look up for a replacement without owner (as the owner sometimes can differ)
            replacements[methodSig.copy(owner = "")]
        }
        when {
            opcode == Opcodes.INVOKEDYNAMIC -> {
                // we don't instrument invokedynamic, because it's just forwarding to a synthetic method
                // which will be instrumented thanks to condition below
                logger.info {
                    "INVOKEDYNAMIC skipped from instrumentation for" +
                        " ${className.prettyPrintClassName()}.${context.name}"
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            }
            replacement != null -> {
                val isSuperCallInOverride = opcode == Opcodes.INVOKESPECIAL &&
                    owner != className &&
                    name == context.name &&
                    descriptor == context.descriptor

                val isSuperCallInCtor = opcode == Opcodes.INVOKESPECIAL &&
                    name == "<init>" &&
                    classContext.superClasses.firstOrNull()?.fqName() == owner

                when {
                    isSuperCallInOverride -> {
                        // this will be instrumented on the calling side of the overriding class
                        logger.info {
                            "${className.prettyPrintClassName()} skipped from instrumentation " +
                                "in overridden method $name.$descriptor"
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                    }
                    isSuperCallInCtor -> {
                        // this has to be manually instrumented (e.g. by inheriting our runtime classes)
                        logger.info {
                            "${className.prettyPrintClassName()} skipped from instrumentation " +
                                "in constructor $name.$descriptor"
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                    }
                    else -> {
                        logger.info {
                            "Wrapping $owner.$name with ${replacement.owner}.${replacement.name} " +
                                "in ${className.prettyPrintClassName()}.${context.name}"
                        }
                        visitWrapping(replacement, opcode, owner, name, descriptor, isInterface)
                    }
                }
            }
            else -> super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }

    private fun String.prettyPrintClassName() = replace('/', '.')

    private fun String.fqName() = replace('.', '/')

    private fun GeneratorAdapter.visitWrapping(
        replacement: Replacement,
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val currentLocal = nextLocal - 1
        // create a new method to figure out the number of arguments
        val originalMethod = Method(name, descriptor)

        // replicate arguments on stack, so we can later re-use them for our wrapping
        val locals = IntArray(originalMethod.argumentTypes.size)
        for (i in locals.size - 1 downTo 0) {
            locals[i] = newLocal(originalMethod.argumentTypes[i])
            storeLocal(locals[i])
        }

        // load arguments from stack for the original method call
        locals.forEach {
            loadLocal(it)
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

        // load arguments from stack for the wrapping method call
        // only load as many as the new method requires (replacement method may have less arguments)
        val newMethod = Method(replacement.name, replacement.descriptor)
        for (i in 0 until newMethod.argumentTypes.size - 1) {
            loadLocal(locals[i])
        }
        // call wrapping (it's always a static method)
        super.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            replacement.owner,
            replacement.name,
            replacement.descriptor,
            false
        )
        if (newWithoutDupInsn && currentLocal > 0) { // 0 is reserved for "this"
            storeLocal(currentLocal)
            newWithoutDupInsn = false
        }
    }
}
