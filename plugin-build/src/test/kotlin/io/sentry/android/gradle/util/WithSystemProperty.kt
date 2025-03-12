package io.sentry.android.gradle.util

/**
 * Annotation to specify arrays of key-values to override [System.getProperties] with
 * [SystemPropertyRule]
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class WithSystemProperty(val keys: Array<String>, val values: Array<String>)
