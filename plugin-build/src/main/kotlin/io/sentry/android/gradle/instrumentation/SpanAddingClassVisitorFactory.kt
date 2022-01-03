package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.gradle.internal.cxx.json.readJsonFile
import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.instrumentation.androidx.room.AndroidXRoomDao
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.AndroidXSQLiteDatabase
import io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.AndroidXSQLiteStatement
import io.sentry.android.gradle.instrumentation.remap.RemappingInstrumentable
import io.sentry.android.gradle.instrumentation.wrap.WrappingInstrumentable
import io.sentry.android.gradle.util.SentryAndroidSdkState
import io.sentry.android.gradle.util.SentryAndroidSdkState.FILE_IO
import io.sentry.android.gradle.util.SentryAndroidSdkState.PERFORMANCE
import io.sentry.android.gradle.util.info
import io.sentry.android.gradle.util.warn
import java.io.File
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
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

        @get:PathSensitive(value = PathSensitivity.NONE)
        @get:InputFile
        val sdkStateFile: RegularFileProperty

        @get:Internal
        val tmpDir: Property<File>

        @get:Internal
        var _instrumentables: List<ClassInstrumentable>?
    }

    private val instrumentables: List<ClassInstrumentable>
        get() {
            val memoized = parameters.get()._instrumentables
            if (memoized != null) {
                SentryPlugin.logger.info { "Memoized: ${memoized.joinToString()}" }
                return memoized
            }

            val sdkState = readJsonFile(
                parameters.get().sdkStateFile.get().asFile,
                SentryAndroidSdkState::class.java
            )
            SentryPlugin.logger.info { "Read sentry-android sdk state: $sdkState" }
            val instrumentables = listOfNotNull(
                AndroidXSQLiteDatabase().takeIf { sdkState.isAtLeast(PERFORMANCE) },
                AndroidXSQLiteStatement().takeIf { sdkState.isAtLeast(PERFORMANCE) },
                AndroidXRoomDao().takeIf { sdkState.isAtLeast(PERFORMANCE) },
                ChainedInstrumentable(
                    listOf(WrappingInstrumentable(), RemappingInstrumentable())
                ).takeIf { sdkState.isAtLeast(FILE_IO) }
            )
            SentryPlugin.logger.info { "Instrumentables: ${instrumentables.joinToString()}" }
            parameters.get()._instrumentables = ArrayList(instrumentables)
            return instrumentables
        }

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
