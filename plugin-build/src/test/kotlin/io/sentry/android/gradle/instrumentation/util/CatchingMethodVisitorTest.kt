package io.sentry.android.gradle.instrumentation.util

import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.fakes.CapturingTestLogger
import kotlin.test.assertEquals
import org.junit.Test
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class CatchingMethodVisitorTest {

    class Fixture {
        private val throwingVisitor = ThrowingMethodVisitor()
        val handler = CapturingExceptionHandler()
        val logger = CapturingTestLogger()

        private val methodContext =
            MethodContext(Opcodes.ACC_PUBLIC, "someMethod", null, null, null)
        val sut
            get() = CatchingMethodVisitor(
                Opcodes.ASM9,
                throwingVisitor,
                "SomeClass",
                methodContext,
                handler,
                logger
            )
    }

    private val fixture = Fixture()

    @Test
    fun `forwards exception to ExceptionHandler`() {
        try {
            fixture.sut.visitMaxs(0, 0)
        } catch (ignored: Throwable) {
        } finally {
            assertEquals(fixture.handler.capturedException!!.message, "This method throws!")
        }
    }

    @Test(expected = CustomException::class)
    fun `rethrows exception`() {
        fixture.sut.visitMaxs(0, 0)
    }

    @Test
    fun `prints message to log`() {
        try {
            fixture.sut.visitMaxs(0, 0)
        } catch (ignored: Throwable) {
        } finally {
            assertEquals(fixture.logger.capturedThrowable!!.message, "This method throws!")
            assertEquals(
                fixture.logger.capturedMessage,
                """
                [sentry] Error while instrumenting SomeClass.someMethod null.
                Please report this issue at https://github.com/getsentry/sentry-android-gradle-plugin/issues
                """.trimIndent()
            )
        }
    }
}

class CustomException : RuntimeException("This method throws!")

class ThrowingMethodVisitor : MethodVisitor(Opcodes.ASM9) {

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        throw CustomException()
    }
}

class CapturingExceptionHandler : ExceptionHandler {

    var capturedException: Throwable? = null

    override fun handle(exception: Throwable) {
        capturedException = exception
    }
}
