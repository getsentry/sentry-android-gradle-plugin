package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.androidx.compose.ComposeNavigation
import io.sentry.android.gradle.instrumentation.androidx.room.AndroidXRoomDao
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.AndroidXSQLiteDatabase
import io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.AndroidXSQLiteStatement
import io.sentry.android.gradle.instrumentation.okhttp.OkHttp
import io.sentry.android.gradle.instrumentation.remap.RemappingInstrumentable
import io.sentry.android.gradle.instrumentation.util.findClassReader
import io.sentry.android.gradle.instrumentation.util.findClassWriter
import io.sentry.android.gradle.instrumentation.util.isMinifiedClass
import io.sentry.android.gradle.instrumentation.wrap.WrappingInstrumentable
import io.sentry.android.gradle.services.SentryModulesService
import io.sentry.android.gradle.util.SemVer
import io.sentry.android.gradle.util.SentryVersions
import io.sentry.android.gradle.util.info
import java.io.File
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor

@Suppress("UnstableApiUsage")
abstract class SpanAddingClassVisitorFactory :
    AsmClassVisitorFactory<SpanAddingClassVisitorFactory.SpanAddingParameters> {

    interface SpanAddingParameters : InstrumentationParameters {

        /**
         * AGP will re-instrument dependencies, when the [InstrumentationParameters] changed
         * https://issuetracker.google.com/issues/190082518#comment4. This is just a dummy parameter
         * that is used solely for that purpose.
         */
        @get:Input
        @get:Optional
        val invalidate: Property<Long>

        @get:Input
        val debug: Property<Boolean>

        @get:Internal
        val sentryModulesService: Property<SentryModulesService>

        @get:Internal
        val tmpDir: Property<File>

        @get:Internal
        var _instrumentable: ClassInstrumentable?
    }

    private val instrumentable: ClassInstrumentable
        get() {
            val memoized = parameters.get()._instrumentable
            if (memoized != null) {
                SentryPlugin.logger.info {
                    "Instrumentable: $memoized [Memoized]"
                }
                return memoized
            }

            val sentryModules = parameters.get().sentryModulesService.get().sentryModules
            val externalModules = parameters.get().sentryModulesService.get().externalModules
            val androidXSqliteFrameWorkModule = DefaultModuleIdentifier.newId(
                "androidx.sqlite",
                "sqlite-framework"
            )
            val androidXSqliteFrameWorkVersion = externalModules.getOrDefault(
                androidXSqliteFrameWorkModule,
                SemVer()
            )

            SentryPlugin.logger.info { "Read sentry modules: $sentryModules" }
            /**
             * When adding a new instrumentable to the list below, do not forget to add a new
             * version to [SentryVersions] if it involves runtime classes
             * from the sentry-android SDK.
             */
            val enabledFeatures = parameters.get()
                .sentryModulesService.get()
                .retrieveEnabledInstrumentationFeatures()

            val instrumentable = ChainedInstrumentable(
                listOfNotNull(
                    AndroidXSQLiteDatabase().takeIf {
                        enabledFeatures.contains(InstrumentationFeature.DATABASE)
                    },
                    AndroidXSQLiteStatement(androidXSqliteFrameWorkVersion).takeIf {
                        enabledFeatures.contains(InstrumentationFeature.DATABASE)
                    },
                    AndroidXRoomDao().takeIf {
                        enabledFeatures.contains(InstrumentationFeature.DATABASE)
                    },
                    OkHttp().takeIf { enabledFeatures.contains(InstrumentationFeature.OKHTTP) },
                    WrappingInstrumentable().takeIf {
                        enabledFeatures.contains(InstrumentationFeature.FILE_IO)
                    },
                    RemappingInstrumentable().takeIf {
                        enabledFeatures.contains(InstrumentationFeature.FILE_IO)
                    },
                    ComposeNavigation().takeIf {
                        enabledFeatures.contains(InstrumentationFeature.COMPOSE)
                    },
                )
            )

            SentryPlugin.logger.info {
                "Instrumentable: $instrumentable"
            }
            parameters.get()._instrumentable = instrumentable
            return instrumentable
        }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val className = classContext.currentClassData.className

        val classReader = nextClassVisitor.findClassWriter()?.findClassReader()
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
            parameters = parameters.get()
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean =
        instrumentable.isInstrumentable(classData.toClassContext())
}
