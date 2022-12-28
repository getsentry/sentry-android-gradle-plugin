package io.sentry.android.gradle.tasks

import io.sentry.android.gradle.util.info
import java.util.UUID
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SentryGenerateIntegrationListTask : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
        description = "Generates a list of auto-applied integrations"
    }

    @get:Input
    abstract val integrations: ListProperty<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun generateProperties() {
        println("!!!! !!!! !!!Action CAlled")
        logger.info {
            "SentryGenerateIntegrationListTask - outputFile: ${output.get()}"
        }

        if (integrations.getOrElse(emptyList()).isNotEmpty()) {
            output.get().asFile.parentFile.mkdirs()
            output.get().asFile.printWriter().use { out ->
                integrations.get().forEach {
                    out.println(it)
                }
            }
        }


//        extensions.ge
//        UUID.randomUUID().also {
//            output.get().asFile.parentFile.mkdirs()
//            output.get().asFile.writeText("io.sentry.ProguardUuids=$it")
//        }
    }
}
