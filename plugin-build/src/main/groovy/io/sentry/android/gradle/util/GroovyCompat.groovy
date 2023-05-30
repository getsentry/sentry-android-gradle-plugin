/*
 * Adapted from https://github.com/bugsnag/bugsnag-android-gradle-plugin/blob/460a71176a990f19f2b6fe61a06f5f138164aa9d/src/main/groovy/com/bugsnag/android/gradle/GroovyCompat.groovy
 *
 * Copyright (c) 2015 Bugsnag
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.sentry.android.gradle.util

import org.gradle.api.Project

/**
 * Contains functions which exploit Groovy's metaprogramming to provide backwards
 * compatibility for older AGP versions that would be impractical to achieve with
 * Kotlin's static type system.
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
