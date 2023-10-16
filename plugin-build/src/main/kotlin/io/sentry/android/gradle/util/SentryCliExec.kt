package io.sentry.android.gradle.util

import io.sentry.android.gradle.util.CliFailureReason.OUTDATED
import java.io.ByteArrayOutputStream
import org.gradle.api.GradleException
import org.gradle.api.tasks.Exec

/**
 * An ext function for tasks that wrap sentry-cli, which provides common error handling. Must be
 * called at configuration phase (=when registering a task).
 */
fun Exec.asSentryCliExec() {
    isIgnoreExitValue = true
    doFirst {
        errorOutput = ByteArrayOutputStream()
    }

    doLast {
        val err = errorOutput.toString()
        if (executionResult.get().exitValue != 0) {
            when (val reason = CliFailureReason.fromErrOut(err)) {
                OUTDATED -> logger.warn { reason.message(name) }
                else -> {
                    logger.lifecycle(err)
                    throw GradleException(reason.message(name))
                }
            }
        } else if (err.isNotEmpty()) {
            logger.lifecycle(err)
        }
    }
}
