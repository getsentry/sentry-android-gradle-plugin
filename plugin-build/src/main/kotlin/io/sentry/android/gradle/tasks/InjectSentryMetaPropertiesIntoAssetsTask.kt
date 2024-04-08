// Adapted from https://github.com/android/gradle-recipes/blob/efeedbc78465547280b5c13b3e04a65b70fa1e26/transformDirectory/build-logic/plugins/src/main/kotlin/TransformAssetsTask.kt
/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.telemetry.SentryTelemetryService
import io.sentry.android.gradle.telemetry.withSentryTelemetry
import io.sentry.android.gradle.util.PropertiesUtil
import java.io.File
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class InjectSentryMetaPropertiesIntoAssetsTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()
        get() {
            // AGP < 8.3 sets an output folder which contains the input folder
            // input:  app/intermediates/assets/release/mergeReleaseAssets
            // output: app/intermediates/assets/release/
            // re-route output to a sub directory instead,
            // as otherwise this breaks the gradle cache functionality

            @Suppress("SENSELESS_COMPARISON")
            if (field == null || !field.isPresent) {
                return field
            }

            if (!field.get().asFile.name.equals(name)) {
                field.set(File(field.get().asFile, name))
            }

            return field
        }

    // we only care about file contents
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val inputPropertyFiles: ConfigurableFileCollection

    @TaskAction
    fun taskAction() {
        val input = inputDir.get().asFile
        val output = outputDir.get().asFile

        if (!output.exists()) {
            output.mkdirs()
        }

        input.copyRecursively(output, overwrite = true)

        // merge props
        val props = Properties()
        props.setProperty("io.sentry.build-tool", "gradle")
        inputPropertyFiles.forEach { inputFile ->
            PropertiesUtil.loadMaybe(inputFile)?.let { props.putAll(it) }
        }

        // write props
        val propsFile = File(output, SENTRY_DEBUG_META_PROPERTIES_OUTPUT)
        propsFile.writer().use {
            props.store(
                it,
                "Generated by sentry-android-gradle-plugin"
            )
        }
    }

    companion object {
        internal const val SENTRY_DEBUG_META_PROPERTIES_OUTPUT = "sentry-debug-meta.properties"

        fun register(
            project: Project,
            extension: SentryPluginExtension,
            sentryTelemetryProvider: Provider<SentryTelemetryService>?,
            tasksGeneratingProperties: List<TaskProvider<out PropertiesFileOutputTask>>,
            taskSuffix: String = ""
        ): TaskProvider<InjectSentryMetaPropertiesIntoAssetsTask> {
            val inputFiles: List<Provider<RegularFile>> = tasksGeneratingProperties.mapNotNull {
                it.flatMap { task -> task.outputFile }
            }
            return project.tasks.register(
                "injectSentryDebugMetaPropertiesIntoAssets$taskSuffix",
                InjectSentryMetaPropertiesIntoAssetsTask::class.java
            ) { task ->
                task.inputPropertyFiles.setFrom(inputFiles)

                task.withSentryTelemetry(extension, sentryTelemetryProvider)
            }
        }
    }
}
