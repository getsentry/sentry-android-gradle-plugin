package io.sentry.android.gradle.instrumentation

internal object InstrumentationBytecodeTestUtil {

  /**
   * Instrumentation bytecode loaded from published AARs on the test classpath:
   * - AndroidX SQLite framework classes: `androidx.sqlite:sqlite-framework`
   * - `TypefaceCompatUtil`: `androidx.core:core`
   */
  private val FIXTURE_TO_RESOURCE =
    mapOf(
      "androidxSqlite/FrameworkSQLiteOpenHelperFactory" to
        "androidx/sqlite/db/framework/FrameworkSQLiteOpenHelperFactory.class",
      "androidxSqlite/FrameworkSQLiteDatabase" to
        "androidx/sqlite/db/framework/FrameworkSQLiteDatabase.class",
      "androidxSqlite/FrameworkSQLiteStatement" to
        "androidx/sqlite/db/framework/FrameworkSQLiteStatement.class",
      "fileIO/TypefaceCompatUtil" to "androidx/core/graphics/TypefaceCompatUtil.class",
    )

  fun hasClasspathFixture(instrumentedProject: String, className: String): Boolean =
    fixtureKey(instrumentedProject, className) in FIXTURE_TO_RESOURCE

  fun loadClasspathFixture(instrumentedProject: String, className: String): ByteArray {
    val fixtureKey = fixtureKey(instrumentedProject, className)
    val resourcePath =
      FIXTURE_TO_RESOURCE[fixtureKey] ?: error("No classpath fixture for $fixtureKey")
    val stream =
      InstrumentationBytecodeTestUtil::class.java.classLoader.getResourceAsStream(resourcePath)
        ?: error(
          "Could not load $resourcePath from test classpath. " +
            "Ensure sqliteFramework and androidxCore are on the test classpath."
        )
    return stream.use { it.readBytes() }
  }

  private fun fixtureKey(instrumentedProject: String, className: String) =
    "$instrumentedProject/$className"
}
