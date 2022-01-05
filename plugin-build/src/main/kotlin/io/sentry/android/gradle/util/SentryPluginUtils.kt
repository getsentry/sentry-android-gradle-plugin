package io.sentry.android.gradle.util

import java.util.Locale
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskProvider

internal object SentryPluginUtils {

    fun withLogging(
        logger: Logger,
        varName: String,
        initializer: () -> TaskProvider<Task>?
    ) = initializer().also {
        logger.info { "$varName is ${it?.name}" }
    }

    fun String.capitalizeUS() = if (isEmpty()) {
        ""
    } else {
        substring(0, 1).toUpperCase(Locale.US) + substring(1)
    }
}
