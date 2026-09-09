package io.sentry.android.gradle.instrumentation.androidx.sqlite.driver

import com.android.build.api.instrumentation.ClassContext
import io.sentry.android.gradle.instrumentation.ClassInstrumentable
import io.sentry.android.gradle.instrumentation.CommonClassVisitor
import io.sentry.android.gradle.instrumentation.MethodContext
import io.sentry.android.gradle.instrumentation.MethodInstrumentable
import io.sentry.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.visitor.SetDriverMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

/** JVM type descriptor for `androidx.sqlite.SQLiteDriver`. */
internal const val SQLITE_DRIVER_TYPE_DESCRIPTOR = "Landroidx/sqlite/SQLiteDriver;"

/**
 * Auto-instruments `SQLiteDriver` for all Room users by wrapping any driver passed to
 * `RoomDatabase.Builder.setDriver(SQLiteDriver)`.
 *
 * In other words, this:
 * ```kotlin
 * val someDriver = AndroidSQLiteDriver()
 * val database = Room.databaseBuilder(context, MyDatabase::class.java, "dbName")
 *      .setDriver(someDriver)
 *      .build()
 * ```
 *
 * becomes:
 * ```kotlin
 * val someDriver = AndroidSQLiteDriver()
 * val database = Room.databaseBuilder(context, MyDatabase::class.java, "dbName")
 *      .setDriver(SentrySQLiteDriver.create(someDriver))
 *      .build()
 * ```
 *
 * `SentrySQLiteDriver` protects against duplicate wrappings, allowing the visitor to wrap
 * `SQLiteDriver` unconditionally.
 *
 * Coverage is limited to Room because SQLDelight
 * [doesn't support `SQLiteDriver`](https://github.com/sqldelight/sqldelight/issues/6072) (it uses
 * `SupportSQLiteOpenHelper`, which we auto-instrument via
 * [AndroidXSQLiteOpenHelper][io.sentry.android.gradle.instrumentation.androidx.sqlite.AndroidXSQLiteOpenHelper]).
 * To keep our implementation simple and build times fast, developers who use `SQLiteDriver`
 * directly are expected to wrap it themselves.
 */
class AndroidXSQLiteDriver : ClassInstrumentable {

  override fun getVisitor(
    instrumentableContext: ClassContext,
    apiVersion: Int,
    originalVisitor: ClassVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): ClassVisitor {
    val currentClassName = instrumentableContext.currentClassData.className

    return CommonClassVisitor(
      apiVersion = apiVersion,
      classVisitor = originalVisitor,
      className = currentClassName.substringAfterLast('.'),
      methodInstrumentables = listOf(SetDriverMethodInstrumentable()),
      parameters = parameters,
    )
  }

  override fun isInstrumentable(data: ClassContext): Boolean =
    data.currentClassData.className in TARGET_CLASSES

  companion object {

    // Currently covers Room 2 and Room 3 packages. Update as needed.
    internal val TARGET_CLASSES =
      setOf("androidx.room.RoomDatabase\$Builder", "androidx.room3.RoomDatabase\$Builder")
  }
}

class SetDriverMethodInstrumentable : MethodInstrumentable {

  override fun getVisitor(
    instrumentableContext: MethodContext,
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
  ): MethodVisitor = SetDriverMethodVisitor(apiVersion, originalVisitor, instrumentableContext)

  override fun isInstrumentable(data: MethodContext): Boolean =
    data.name == SET_DRIVER && data.descriptor?.startsWith(SET_DRIVER_DESCRIPTOR_PREFIX) == true

  companion object {
    internal const val SET_DRIVER = "setDriver"
    internal const val SET_DRIVER_DESCRIPTOR_PREFIX = "($SQLITE_DRIVER_TYPE_DESCRIPTOR)"
  }
}
