package io.sentry.android.gradle.util

import org.slf4j.Logger

fun Logger.warn(throwable: Throwable? = null, message: () -> String) {
    warn("[sentry] ${message()}", throwable)
}

fun Logger.error(throwable: Throwable? = null, message: () -> String) {
    error("[sentry] ${message()}", throwable)
}

fun Logger.debug(throwable: Throwable? = null, message: () -> String) {
    debug("[sentry] ${message()}", throwable)
}

fun Logger.info(throwable: Throwable? = null, message: () -> String) {
    info("[sentry] ${message()}", throwable)
}


