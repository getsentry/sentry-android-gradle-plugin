package io.sentry.android.gradle.instrumentation.okhttp.visitor

import io.sentry.android.gradle.instrumentation.util.Types
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

class ResponseWithInterceptorChainMethodVisitor(
    private val useSentryAndroidOkHttp: Boolean,
    api: Int,
    private val originalVisitor: MethodVisitor,
    access: Int,
    name: String?,
    descriptor: String?
) : GeneratorAdapter(api, originalVisitor, access, name, descriptor) {

    private var shouldInstrument = false

    private val sentryOkInterceptor = if (useSentryAndroidOkHttp) Types.SENTRY_ANDROID_OKHTTP_INTERCEPTOR else Types.SENTRY_OKHTTP_INTERCEPTOR

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        if (opcode == Opcodes.INVOKEVIRTUAL &&
            owner == "okhttp3/OkHttpClient" &&
            name == "interceptors"
        ) {
            shouldInstrument = true
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
        if (opcode == Opcodes.POP && shouldInstrument) {
            visitAddSentryInterceptor()
            shouldInstrument = false
        }
    }

    /*
     Roughly constructing this, but in Java:

     if (interceptors.find { it is SentryOkHttpInterceptor } != null) {
       interceptors += SentryOkHttpInterceptor()
     }
     */
    private fun MethodVisitor.visitAddSentryInterceptor() {
        originalVisitor.visitVarInsn(Opcodes.ALOAD, 1) // interceptors list

        checkCast(Types.ITERABLE)
        invokeInterface(Types.ITERABLE, Method.getMethod("java.util.Iterator iterator ()"))
        val iteratorIndex = newLocal(Types.ITERATOR)
        storeLocal(iteratorIndex)

        val whileLabel = Label()
        val endWhileLabel = Label()
        visitLabel(whileLabel)
        loadLocal(iteratorIndex)
        invokeInterface(Types.ITERATOR, Method.getMethod("boolean hasNext ()"))
        ifZCmp(EQ, endWhileLabel)
        loadLocal(iteratorIndex)
        invokeInterface(Types.ITERATOR, Method.getMethod("Object next ()"))

        val interceptorIndex = newLocal(Types.OBJECT)
        storeLocal(interceptorIndex)
        loadLocal(interceptorIndex)
        checkCast(Types.OKHTTP_INTERCEPTOR)
        instanceOf(sentryOkInterceptor)
        ifZCmp(EQ, whileLabel)
        loadLocal(interceptorIndex)
        val ifLabel = Label()
        goTo(ifLabel)

        visitLabel(endWhileLabel)
        originalVisitor.visitInsn(Opcodes.ACONST_NULL)
        visitLabel(ifLabel)
        val originalMethodLabel = Label()
        ifNonNull(originalMethodLabel)

        originalVisitor.visitVarInsn(Opcodes.ALOAD, 1)
        checkCast(Types.COLLECTION)
        newInstance(sentryOkInterceptor)
        dup()
        val sentryOkHttpCtor = Method.getMethod("void <init> ()")
        invokeConstructor(sentryOkInterceptor, sentryOkHttpCtor)
        val addInterceptor = Method.getMethod("boolean add (Object)")
        invokeInterface(Types.COLLECTION, addInterceptor)
        pop()
        visitLabel(originalMethodLabel)
    }
}
