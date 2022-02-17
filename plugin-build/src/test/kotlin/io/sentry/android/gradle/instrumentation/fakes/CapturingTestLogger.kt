package io.sentry.android.gradle.instrumentation.fakes

class CapturingTestLogger : BaseTestLogger() {
    override fun getName(): String = "SentryPluginTest"

    var capturedMessage: String? = null
    var capturedThrowable: Throwable? = null

    override fun error(msg: String, throwable: Throwable?) {
        capturedMessage = msg
        capturedThrowable = throwable
    }

    override fun warn(msg: String, throwable: Throwable?) {
        capturedMessage = msg
        capturedThrowable = throwable
    }

    override fun info(msg: String, throwable: Throwable?) {
        capturedMessage = msg
        capturedThrowable = throwable
    }
}
