package io.sentry.android.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@Suppress("LeakingThis", "UnstableApiUsage")
abstract class SentryProguardConfigTask : DefaultTask() {
    @get:Input
    abstract val generateFile: Property<Boolean>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        description = "Adds the Sentry recommended proguard settings to your project."
        group = "sentry"

        outputFile.finalizeValueOnRead()
        generateFile.finalizeValueOnRead()

        onlyIf { generateFile.get() }
    }

    @TaskAction
    fun createProguardConfig() {
        //language=PROGUARD
        outputFile.get().asFile.writeText("""
            -keepattributes LineNumberTable,SourceFile
            -dontwarn com.facebook.fbui.**
            -dontwarn org.slf4j.**
            -dontwarn javax.**
            -keep class * extends java.lang.Exception
            -keep class io.sentry.event.Event { *; }
        """.trimIndent())
    }
}