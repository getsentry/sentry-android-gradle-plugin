package io.sentry.android.gradle.util

import java.io.File
import java.io.Writer
import java.util.Properties

class PropertiesUtil {
  companion object {
    fun load(file: File): Properties {
      check(file.exists()) { "${file.name} properties file is missing" }

      return Properties().also { properties -> file.inputStream().use { properties.load(it) } }
    }

    fun loadMaybe(file: File): Properties? {
      if (!file.exists()) {
        return null
      }

      return load(file)
    }

    /**
     * Stores properties to a writer without timestamps, ensuring reproducible builds.
     *
     * @param props The properties to store
     * @param writer The writer to write to
     * @param comment Optional comment to include at the top of the file (without the # prefix)
     */
    fun store(props: Properties, writer: Writer, comment: String? = null) {
      comment?.let { writer.write("# $it\n") }
      props.stringPropertyNames().sorted().forEach { key ->
        writer.write("$key=${props.getProperty(key)}\n")
      }
    }
  }
}
