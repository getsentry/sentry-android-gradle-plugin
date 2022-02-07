package io.sentry.android.gradle.util

import com.android.build.gradle.internal.cxx.json.writeJsonFile
import io.sentry.android.gradle.SentryPlugin
import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult

fun Project.detectSentryAndroidSdk(
    configurationName: String,
    variantName: String
) {
    val configProvider = configurations.named(configurationName)

    if (!configProvider.isPresent) {
        logger.warn {
            "Unable to find configuration $configurationName for variant $variantName."
        }
        writeJsonFile(
            project.file(File(project.buildDir, SentryPlugin.buildSdkStateFilePath(variantName))),
            SentryAndroidSdkState.MISSING
        )
        return
    }

    configProvider.configure { configuration ->
        configuration.incoming.afterResolve {
            val version = it.resolutionResult.allComponents.findSentryAndroidSdk()
            if (version == null) {
                logger.warn { "sentry-android dependency was not found." }
                writeJsonFile(
                    project.file(
                        File(project.buildDir, SentryPlugin.buildSdkStateFilePath(variantName))
                    ),
                    SentryAndroidSdkState.MISSING
                )
                return@afterResolve
            }

            val state = try {
                val sdkState = SentryAndroidSdkState.from(version)
                logger.info {
                    "Detected sentry-android $sdkState for version: $version, " +
                        "variant: $variantName, config: $configurationName"
                }
                sdkState
            } catch (e: IllegalStateException) {
                logger.warn { e.localizedMessage }
                SentryAndroidSdkState.MISSING
            }
            writeJsonFile(
                project.file(
                    File(project.buildDir, SentryPlugin.buildSdkStateFilePath(variantName))
                ),
                state
            )
        }
    }
}

private fun Set<ResolvedComponentResult>.findSentryAndroidSdk(): String? {
    val sentryDep = find { resolvedComponent: ResolvedComponentResult ->
        val moduleVersion = resolvedComponent.moduleVersion ?: return@find false
        moduleVersion.group == "io.sentry" && moduleVersion.name == "sentry-android-core"
    }
    return sentryDep?.moduleVersion?.version
}
