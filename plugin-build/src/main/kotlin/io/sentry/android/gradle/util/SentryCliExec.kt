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
  // this is a workaround, otherwise doFirst is not needed
  // https://github.com/gradle/gradle/issues/16535
  doFirst { errorOutput = ByteArrayOutputStream() }

  doLast {
    val err = errorOutput.toString()
    val exitValue = executionResult.orNull?.exitValue ?: 0
    if (exitValue != 0) {
      when (val reason = CliFailureReason.fromErrOut(err)) {
        OUTDATED -> logger.warn { reason.message(name) }
        else -> {
          logger.lifecycle(err)
          throw SentryCliException(reason, name)
        }
      }
    } else if (err.isNotEmpty()) {
      logger.lifecycle(err)
    }
  }
}

open class SentryCliException(val reason: CliFailureReason, name: String) :
  GradleException(reason.message(name))
