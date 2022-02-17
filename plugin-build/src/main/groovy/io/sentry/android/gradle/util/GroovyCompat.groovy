package io.sentry.android.gradle.util

import org.gradle.api.Project

/**
 * Contains functions which exploit Groovy's metaprogramming to provide backwards
 * compatibility for older AGP versions that would be impractical to achieve with
 * Kotlin's static type system.
 *
 * Adapted under MIT license from:
 * https://github.com/bugsnag/bugsnag-android-gradle-plugin/blob/master/src/main/groovy/com/bugsnag/android/gradle/GroovyCompat.groovy
 */
class GroovyCompat {

    static boolean isDexguardAvailable(Project project) {
        return project.extensions.findByName("dexguard") != null
    }

    static boolean isDexguardEnabledForVariant(Project project, String variantName) {
        def dexguard = project.extensions.findByName("dexguard")

        try {
            if (dexguard == null) {
                return null
            }

            if (dexguard.configurations != null) {
                return dexguard.configurations.findByName(variantName) != null
            } else {
                // no configurations = assume all variants configured
                return true
            }
        } catch (MissingPropertyException ignored) {
            // running earlier version of DexGuard, ignore missing property
            return false
        }
    }
}
