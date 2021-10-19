package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.sentry.android.gradle.instrumentation.androidx.room.AndroidXRoomDao
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.AndroidXSQLiteDatabase
import io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.AndroidXSQLiteStatement
import java.io.File
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor
import org.slf4j.LoggerFactory

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
        val tmpDir: Property<File>
    }

    companion object {
        private val instrumentables: MutableList<ClassInstrumentable> = mutableListOf(
            AndroidXSQLiteDatabase(),
            AndroidXSQLiteStatement(),
            AndroidXRoomDao()
        )
    }

    private val logger by lazy { LoggerFactory.getLogger(this::class.java) }

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
                logger.warn(
                    """
                    ${classContext.currentClassData.className} is not supported for instrumentation.
                    This is likely a bug, please file an issue at https://github.com/getsentry/sentry-android-gradle-plugin/issues
                    """.trimIndent()
                )
            }

    override fun isInstrumentable(classData: ClassData): Boolean =
        instrumentables.any { it.isInstrumentable(classData.toClassContext()) }
}
