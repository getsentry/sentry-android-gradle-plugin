package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.sentry.android.gradle.InstrumentationFeature
import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.instrumentation.androidx.room.AndroidXRoomDao
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.AndroidXSQLiteDatabase
import io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.AndroidXSQLiteStatement
import io.sentry.android.gradle.instrumentation.remap.RemappingInstrumentable
import io.sentry.android.gradle.instrumentation.wrap.WrappingInstrumentable
import io.sentry.android.gradle.services.SentrySdkStateHolder
import io.sentry.android.gradle.util.SentryAndroidSdkState
import io.sentry.android.gradle.util.SentryAndroidSdkState.FILE_IO
import io.sentry.android.gradle.util.SentryAndroidSdkState.PERFORMANCE
import io.sentry.android.gradle.util.debug
import io.sentry.android.gradle.util.info
import io.sentry.android.gradle.util.warn
import java.io.File
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
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

        @get:Input
        val features: SetProperty<InstrumentationFeature>

        @get:Internal
        val sdkStateHolder: Property<SentrySdkStateHolder>

        @get:Internal
        val tmpDir: Property<File>

        @get:Internal
        var _instrumentables: List<ClassInstrumentable>?
    }

    private val instrumentables: List<ClassInstrumentable>
        get() {
            val memoized = parameters.get()._instrumentables
            if (memoized != null) {
                SentryPlugin.logger.debug {
                    "Memoized: ${memoized.joinToString { it::class.java.simpleName }}"
                }
                return memoized
            }

            val sdkState = parameters.get().sdkStateHolder.get().sdkState
            SentryPlugin.logger.info { "Read sentry-android sdk state: $sdkState" }
            /**
             * When adding a new instrumentable to the list below, do not forget to add a new
             * version range to [SentryAndroidSdkState.from], if it involves runtime classes
             * from the sentry-android SDK.
             */
            val instrumentables = listOfNotNull(
                AndroidXSQLiteDatabase().takeIf {
                    isDatabaseInstrEnabled(sdkState, parameters.get())
                },
                AndroidXSQLiteStatement().takeIf {
                    isDatabaseInstrEnabled(sdkState, parameters.get())
                },
                AndroidXRoomDao().takeIf { isDatabaseInstrEnabled(sdkState, parameters.get()) },
                ChainedInstrumentable(
                    listOf(WrappingInstrumentable(), RemappingInstrumentable())
                ).takeIf { isFileIOInstrEnabled(sdkState, parameters.get()) }
            )
            SentryPlugin.logger.debug {
                "Instrumentables: ${instrumentables.joinToString { it::class.java.simpleName }}"
            }
            parameters.get()._instrumentables = ArrayList(instrumentables)
            return instrumentables
        }

    private fun isDatabaseInstrEnabled(
        sdkState: SentryAndroidSdkState,
        parameters: SpanAddingParameters
    ): Boolean =
        sdkState.isAtLeast(PERFORMANCE) &&
            parameters.features.get().contains(InstrumentationFeature.DATABASE)

    private fun isFileIOInstrEnabled(
        sdkState: SentryAndroidSdkState,
        parameters: SpanAddingParameters
    ): Boolean =
        sdkState.isAtLeast(FILE_IO) &&
            parameters.features.get().contains(InstrumentationFeature.FILE_IO)

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor =
        instrumentables.find { it.isInstrumentable(classContext) }
            ?.getVisitor(
                classContext,
                instrumentationContext.apiVersion.get(),
                nextClassVisitor,
                parameters = parameters.get()
            )
            ?: nextClassVisitor.also {
                SentryPlugin.logger.warn {
                    """
                    ${classContext.currentClassData.className} is not supported for instrumentation.
                    This is likely a bug, please file an issue at https://github.com/getsentry/sentry-android-gradle-plugin/issues
                    """.trimIndent()
                }
            }

    override fun isInstrumentable(classData: ClassData): Boolean =
        instrumentables.any { it.isInstrumentable(classData.toClassContext()) }
}
