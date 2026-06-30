package io.sentry.android.gradle.instrumentation

internal object InstrumentationBytecodeTestUtil {

  fun loadClasspathFixture(className: String): ByteArray? {
    val resourcePath = className.replace('.', '/') + ".class"
    return InstrumentationBytecodeTestUtil::class
      .java
      .classLoader
      .getResourceAsStream(resourcePath)
      ?.use { it.readBytes() }
  }
}
