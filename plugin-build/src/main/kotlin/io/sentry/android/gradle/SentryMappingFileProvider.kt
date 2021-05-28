package io.sentry.android.gradle

import com.android.build.gradle.api.ApplicationVariant
import io.sentry.Sentry
import java.io.File
import org.gradle.api.Project

internal object SentryMappingFileProvider {

    /**
     * Returns the obfuscation mapping file (might be null if the obfuscation is disabled).
     *
     * @return the mapping file or null if not found
     */
    @JvmStatic
    fun getMappingFile(project: Project, variant: ApplicationVariant): File? =
        try {
            if (!variant.mappingFileProvider.isPresent) {
                project.logger.info(
                    "[sentry] .mappingFileProvider is missing for $variant"
                )
                null
            } else {
                val mappingFiles = variant.mappingFileProvider.get().files
                val logMessage = if (mappingFiles.isEmpty()) {
                    "[sentry] .mappingFileProvider.files is empty for ${variant.name}"
                } else {
                    "[sentry] Mapping File ${mappingFiles.first()} for ${variant.name}"
                }
                project.logger.info(logMessage)
                mappingFiles.firstOrNull()
            }
        } catch (throwable: Throwable) {
            project.logger.info(
                "[sentry] .mappingFileProvider is missing for $variant - Error: ${throwable.message}",
                throwable
            )
            Sentry.captureException(throwable)
            null
        }
}
