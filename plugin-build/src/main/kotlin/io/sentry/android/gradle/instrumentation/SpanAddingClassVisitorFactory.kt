package io.sentry.android.gradle.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.sentry.android.gradle.instrumentation.androidx.sqlite.database.AndroidXSQLiteDatabase
import io.sentry.android.gradle.instrumentation.androidx.sqlite.statement.AndroidXSQLiteStatement
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor

@Suppress("UnstableApiUsage")
abstract class SpanAddingClassVisitorFactory :
    AsmClassVisitorFactory<SpanAddingClassVisitorFactory.SpanAddingParameters> {

    companion object {
        private val instrumentables: List<Instrumentable<ClassVisitor>> = listOf(
            AndroidXSQLiteDatabase(),
            AndroidXSQLiteStatement()
        )
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor =
        instrumentables.find { it.fqName == classContext.currentClassData.className }
            ?.getVisitor(
                instrumentationContext.apiVersion.get(),
                nextClassVisitor,
                parameters = parameters.get()
            )
            ?: error(
                "${classContext.currentClassData.className} is not supported for instrumentation"
            )

    override fun isInstrumentable(classData: ClassData): Boolean =
        classData.className in instrumentables.map { it.fqName }

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
        val tmpDir: RegularFileProperty
    }
}
