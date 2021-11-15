package io.sentry.android.gradle.instrumentation.util

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.util.error
import org.objectweb.asm.MethodVisitor

interface ExceptionHandler {
    fun handle(exception: Exception)
}

class CatchingMethodVisitor(
    apiVersion: Int,
    prevVisitor: MethodVisitor,
    private val className: String,
    private val methodContext: MethodContext,
    private val exceptionHandler: ExceptionHandler? = null
) : MethodVisitor(apiVersion, prevVisitor) {

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        try {
            super.visitMaxs(maxStack, maxLocals)
        } catch (e: Exception) {
            exceptionHandler?.handle(e)
            SentryPlugin.logger.error {
                """
                Error while instrumenting $className.${methodContext.name} ${methodContext.descriptor}.
                Please report this issue at https://github.com/getsentry/sentry-android-gradle-plugin/issues
                """.trimIndent()
            }
            throw e
        }
    }
}
