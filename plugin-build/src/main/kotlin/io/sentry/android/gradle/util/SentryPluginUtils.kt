package io.sentry.android.gradle.util

import java.util.Locale
import org.gradle.api.Task
import org.gradle.api.logging.Logger

internal object SentryPluginUtils {

    fun withLogging(
        logger: Logger,
        varName: String,
        initializer: () -> Task?
    ) = initializer().also {
        logger.info("[sentry] $varName is ${it?.path}")
    }
}

fun String.capitalizeUS() = if (isEmpty()) {
    ""
} else {
    substring(0, 1).toUpperCase(Locale.US) + substring(1)
}
