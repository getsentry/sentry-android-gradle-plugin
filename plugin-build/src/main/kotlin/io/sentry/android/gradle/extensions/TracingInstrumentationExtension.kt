package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

open class TracingInstrumentationExtension @Inject constructor(objects: ObjectFactory) {
  /**
   * Enable the tracing instrumentation. Does bytecode manipulation for specified [features].
   * Defaults to true.
   */
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

  /**
   * Enabled debug output of the plugin. Useful when there are issues with code instrumentation,
   * shows the modified bytecode. Defaults to false.
   */
  val debug: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  /**
   * Forces dependencies instrumentation, even if they were already instrumented. Useful when there
   * are issues with code instrumentation, e.g. the dependencies are partially instrumented.
   * Defaults to false.
   */
  val forceInstrumentDependencies: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /**
   * Specifies a set of [InstrumentationFeature] features that are eligible for bytecode
   * manipulation. Defaults to all available features of [InstrumentationFeature].
   */
  val features: SetProperty<InstrumentationFeature> =
    objects
      .setProperty(InstrumentationFeature::class.java)
      .convention(
        setOf(
          InstrumentationFeature.DATABASE,
          InstrumentationFeature.FILE_IO,
          InstrumentationFeature.OKHTTP,
          InstrumentationFeature.COMPOSE,
        )
      )

  /**
   * The set of glob patterns to exclude from instrumentation. Classes matching any of these
   * patterns in the project sources and dependencies jars do not get instrumented by the Sentry
   * Gradle plugin.
   *
   * Do not add the file extension to the end as the filtration is done on compiled classes and the
   * .class suffix is not included in the pattern matching.
   *
   * Example usage:
   * ```
   * excludes.set(setOf("com/example/donotinstrument/**", "**/*Test"))
   * ```
   *
   * Only supported when using Android Gradle plugin (AGP) version 7.4.0 and above.
   */
  val excludes: SetProperty<String> = objects.setProperty(String::class.java).convention(emptySet())

  val logcat: LogcatExtension = objects.newInstance(LogcatExtension::class.java)

  fun logcat(logcatAction: Action<LogcatExtension>) {
    logcatAction.execute(logcat)
  }

  val appStart: AppStartExtension = objects.newInstance(AppStartExtension::class.java)

  fun appStart(appStartExtensionAction: Action<AppStartExtension>) {
    appStartExtensionAction.execute(appStart)
  }
}

enum class InstrumentationFeature(val integrationName: String) {
  /**
   * When enabled the SDK will create spans for any CRUD operation performed by
   * 'androidx.sqlite.db.SupportSQLiteOpenHelper' and 'androidx.room'. This feature uses bytecode
   * manipulation.
   */
  DATABASE("DatabaseInstrumentation"),

  /**
   * When enabled the SDK will create spans for [java.io.FileInputStream],
   * [java.io.FileOutputStream], [java.io.FileReader], [java.io.FileWriter]. This feature uses
   * bytecode manipulation and replaces the above mentioned classes with Sentry-specific
   * implementations.
   */
  FILE_IO("FileIOInstrumentation"),

  /**
   * When enabled the SDK will create spans for outgoing network requests and attach
   * sentry-trace-header for distributed tracing. This feature uses bytecode manipulation and
   * attaches SentryOkHttpInterceptor to all OkHttp clients in the project.
   */
  OKHTTP("OkHttpInstrumentation"),

  /**
   * When enabled the SDK will create breadcrumbs when navigating using
   * [androidx.navigation.NavController]. This feature uses bytecode manipulation and adds an
   * OnDestinationChangedListener to all navigation controllers used in Jetpack Compose.
   */
  COMPOSE("ComposeInstrumentation"),
}
