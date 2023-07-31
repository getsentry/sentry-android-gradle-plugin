package io.sentry.android.gradle.util

import java.io.File
import java.util.Properties

class PropertiesUtil {
    companion object {
        fun load(file: File): Properties {
            check(file.exists()) {
                "${file.name} properties file is missing"
            }

            return Properties().also { properties ->
                file.inputStream().use {
                    properties.load(it)
                }
            }
        }

        fun loadMaybe(file: File): Properties? {
            if (!file.exists()) {
                return null
            }

            return load(file)
        }
    }
}
