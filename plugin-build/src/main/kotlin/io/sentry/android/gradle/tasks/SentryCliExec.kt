package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.CliFailureReason
import io.sentry.android.gradle.util.CliFailureReason.OUTDATED
import io.sentry.android.gradle.util.warn
import java.io.ByteArrayOutputStream
import org.gradle.api.GradleException
import org.gradle.api.tasks.Exec

/**
 * A base class for tasks that wrap sentry-cli, which provides common error handling.
 */
abstract class SentryCliExec : Exec() {
    init {
        errorOutput = ByteArrayOutputStream()
        isIgnoreExitValue = true

        @Suppress("LeakingThis")
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
}
