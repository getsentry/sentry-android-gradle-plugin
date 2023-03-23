package io.sentry.android.gradle.instrumentation.logcat

enum class LogcatLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR;

    fun allowedLogFunctions(): List<String> {
        return values()
            .filter { it.ordinal >= this.ordinal }
            .flatMap { it.functionNames() }
    }

    private fun functionNames(): List<String> {
        return when (this) {
            VERBOSE -> listOf("v")
            DEBUG -> listOf("d")
            INFO -> listOf("i")
            WARNING -> listOf("w")
            ERROR -> listOf("e", "wtf")
        }
    }
}
