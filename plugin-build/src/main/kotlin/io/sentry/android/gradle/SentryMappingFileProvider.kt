package io.sentry.android.gradle

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Project
import java.io.File

internal object SentryMappingFileProvider {

    /**
     * Returns the obfuscation mapping file (might be null if the obfuscation is disabled).
     *
     * @return the mapping file or null if not found
     */
    @JvmStatic
    fun getMappingFile(project: Project, variant: ApplicationVariant): File? =
        try {
            val mappingFiles = variant.mappingFileProvider.get().files
            if (mappingFiles.isEmpty()) {
                project.logger.warn("[sentry] .mappingFileProvider.files is empty for ${variant.name}")
                null
            } else {
                project.logger.info("[sentry] Mapping File ${mappingFiles.first()} for ${variant.name}")
                mappingFiles.first()
            }
        } catch (ignored: Throwable) {
            project.logger.error("[sentry] .mappingFileProvider failed with: ${ignored.message}")
            variant.mappingFile
        }
}
