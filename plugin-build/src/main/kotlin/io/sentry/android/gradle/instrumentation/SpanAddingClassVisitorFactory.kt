package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.androidx.compose.ComposeNavigation
import io.sentry.android.gradle.instrumentation.androidx.room.AndroidXRoomDao
import io.sentry.android.gradle.instrumentation.androidx.sqlite.AndroidXSQLiteOpenHelper
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.AndroidXSQLiteDatabase
import io.sentry.android.gradle.instrumentation.androidx.sqlite.driver.AndroidXSQLiteDriver
import io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.AndroidXSQLiteStatement
import io.sentry.android.gradle.instrumentation.appstart.Application
import io.sentry.android.gradle.instrumentation.appstart.ContentProvider
import io.sentry.android.gradle.instrumentation.logcat.Logcat
import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import io.sentry.android.gradle.instrumentation.okhttp.OkHttp
import io.sentry.android.gradle.instrumentation.okhttp.OkHttpEventListener
import io.sentry.android.gradle.instrumentation.remap.RemappingInstrumentable
import io.sentry.android.gradle.instrumentation.util.findClassReader
import io.sentry.android.gradle.instrumentation.util.findClassWriter
import io.sentry.android.gradle.instrumentation.util.isMinifiedClass
import io.sentry.android.gradle.instrumentation.util.raiseComputeLimits
import io.sentry.android.gradle.instrumentation.wrap.WrappingInstrumentable
import io.sentry.android.gradle.util.ExternalModules
import io.sentry.android.gradle.util.SentryModuleVersions
import io.sentry.android.gradle.util.SentryModules
import io.sentry.android.gradle.util.SentryVersions
import io.sentry.android.gradle.util.info
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor

abstract class SpanAddingClassVisitorFactory :
  AsmClassVisitorFactory<SpanAddingClassVisitorFactory.SpanAddingParameters> {

  interface SpanAddingParameters : InstrumentationParameters {

    /**
     * AGP will re-instrument dependencies, when the [InstrumentationParameters] changed
     * https://issuetracker.google.com/issues/190082518#comment4. This is just a dummy parameter
     * that is used solely for that purpose.
     */
    @get:Input @get:Optional val invalidate: Property<Long>

    @get:Input val debug: Property<Boolean>

    @get:Input val logcatMinLevel: Property<LogcatLevel>

    /**
     * Resolved versions of the modules the instrumentation branches on, keyed by `"group:name"`. An
     * `@Input` so that it keys the build cache, and so Gradle recomputes it when it reuses a
     * configuration cache entry.
     */
    @get:Input val moduleVersions: MapProperty<String, String>

    @get:Internal val tmpDir: DirectoryProperty

    @get:Internal var _instrumentable: ClassInstrumentable?

    @get:Input val features: SetProperty<InstrumentationFeature>

    @get:Input val logcatEnabled: Property<Boolean>

    @get:Input val appStartEnabled: Property<Boolean>
  }

  private val instrumentable: ClassInstrumentable
    get() {
      val memoized = parameters.get()._instrumentable
      if (memoized != null) {
        return memoized
      }

      val params = parameters.get()
      val versions = SentryModuleVersions(params.moduleVersions.get())
      val enabled =
        EnabledInstrumentations(
          versions,
          params.features.get(),
          params.logcatEnabled.get(),
          params.appStartEnabled.get(),
        )

      val androidXSqliteFrameWorkVersion = versions[ExternalModules.ANDROIDX_SQLITE_FRAMEWORK]
      val okHttpVersion = versions[ExternalModules.OKHTTP]
      val useSentryAndroidOkHttp =
        versions[SentryModules.SENTRY_OKHTTP] < SentryVersions.VERSION_OKHTTP

      SentryPlugin.logger.info { "Read sentry modules: ${versions.versions}" }

      val instrumentable =
        ChainedInstrumentable(
          listOfNotNull(
            AndroidXSQLiteOpenHelper().takeIf { enabled.newDatabase },
            AndroidXSQLiteDriver().takeIf { enabled.sqliteDriver },
            AndroidXSQLiteDatabase().takeIf { enabled.oldDatabase },
            AndroidXSQLiteStatement(androidXSqliteFrameWorkVersion).takeIf { enabled.oldDatabase },
            // Note that DAO spans no longer work on Room 2.7+ or Room 3.0+ due to Room API changes.
            AndroidXRoomDao().takeIf { enabled.newDatabase || enabled.oldDatabase },
            OkHttpEventListener(useSentryAndroidOkHttp, okHttpVersion).takeIf {
              enabled.okHttpListener
            },
            OkHttp(useSentryAndroidOkHttp).takeIf { enabled.okHttp },
            WrappingInstrumentable().takeIf { enabled.fileIO },
            RemappingInstrumentable().takeIf { enabled.fileIO },
            ComposeNavigation().takeIf { enabled.compose },
            Logcat().takeIf { enabled.logcat },
            Application().takeIf { enabled.appStart },
            ContentProvider().takeIf { enabled.appStart },
          )
        )
      SentryPlugin.logger.info { "Instrumentable: $instrumentable" }
      params._instrumentable = instrumentable
      return instrumentable
    }

  override fun createClassVisitor(
    classContext: ClassContext,
    nextClassVisitor: ClassVisitor,
  ): ClassVisitor {
    val className = classContext.currentClassData.className

    val classWriter = nextClassVisitor.findClassWriter()
    classWriter?.raiseComputeLimits()
    val classReader = classWriter?.findClassReader()
    val isMinifiedClass = classReader?.isMinifiedClass() ?: false
    if (isMinifiedClass) {
      SentryPlugin.logger.info {
        "$className skipped from instrumentation because it's a minified class."
      }
      return nextClassVisitor
    }

    return instrumentable.getVisitor(
      classContext,
      instrumentationContext.apiVersion.get(),
      nextClassVisitor,
      parameters = parameters.get(),
    )
  }

  override fun isInstrumentable(classData: ClassData): Boolean =
    instrumentable.isInstrumentable(classData.toClassContext())
}
