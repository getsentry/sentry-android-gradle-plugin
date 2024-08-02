package io.sentry.android.gradle.instrumentation.fakes

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

/**
 * Prints each method that is being visited in the class useful when a Class is failing Java
 * bytecode verifier, but not clear which method is causing it
 */
class MethodNamePrintingVisitor(api: Int, originalVisitor: ClassVisitor) :
  ClassVisitor(api, originalVisitor) {

  override fun visitMethod(
    access: Int,
    name: String?,
    descriptor: String?,
    signature: String?,
    exceptions: Array<out String>?,
  ): MethodVisitor {
    println(name)
    return super.visitMethod(access, name, descriptor, signature, exceptions)
  }
}
