package io.sentry.android.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.Version
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadNativeSymbolsTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.VersionNumber

class SentryPlugin implements Plugin<Project> {
    static final String GROUP_NAME = 'Sentry'
    private static final String SENTRY_ORG_PARAMETER = "sentryOrg"
    private static final String SENTRY_PROJECT_PARAMETER = "sentryProject"

    void apply(Project project) {
        SentryPluginExtension extension = project.extensions.create("sentry", SentryPluginExtension, project)

        project.afterEvaluate {
            if (!project.plugins.hasPlugin(AppPlugin) && !project.getPlugins().hasPlugin(LibraryPlugin)) {
                throw new IllegalStateException('Must apply \'com.android.application\' first!')
            }

            project.android.applicationVariants.all { ApplicationVariant variant ->
                variant.outputs.each { variantOutput ->

                    def mappingFile = SentryMappingFileProvider.getMappingFile(project, variant)
                    def transformerTask = SentryTasksProvider.getTransformerTask(project, variant.name)

                    def dexTask = SentryTasksProvider.getDexTask(project, variant.name)
                    if (dexTask != null) {
                        project.logger.info("dexTask ${dexTask.path}")
                    } else {
                        project.logger.info("dexTask is null")
                    }

                    def preBundleTask = SentryTasksProvider.getPreBundleTask(project, variant.name)
                    if (preBundleTask != null) {
                        project.logger.info("preBundleTask ${preBundleTask.path}")
                    } else {
                        project.logger.info("preBundleTask is null")
                    }

                    def bundleTask = SentryTasksProvider.getBundleTask(project, variant.name)
                    if (bundleTask != null) {
                        project.logger.info("bundleTask ${bundleTask.path}")
                    } else {
                        project.logger.info("bundleTask is null")
                    }

                    if (transformerTask == null) {
                        project.logger.info("transformerTask is null")
                        return
                    } else {
                        project.logger.info("transformerTask ${transformerTask.path}")
                    }

                    def cli = SentryCliProvider.getSentryCliPath(project)

                    def generateUuidTask = project.tasks.create(
                            name: "generateSentryProguardUuid${variant.name.capitalize()}${variantOutput.name.capitalize()}",
                            type: SentryGenerateProguardUuidTask) {
                        outputDirectory.set(project.file("build/generated/assets/sentry/${variant.name}"))

                        doFirst {
                            project.logger.info("debugMetaPropPath: ${getOutputFile().get()}")
                        }
                    }

                    if (VersionNumber.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION) >= new VersionNumber(3, 3, 0, null)) {
                        variant.mergeAssetsProvider.configure {
                            dependsOn(generateUuidTask)
                        }
                    } else {
                        //noinspection GrDeprecatedAPIUsage
                        variant.mergeAssets.dependsOn(generateUuidTask)
                    }

                    // create a task that uploads the proguard mapping and UUIDs
                    def uploadSentryProguardMappingsTask = project.tasks.create(
                            name: "uploadSentryProguardMappings${variant.name.capitalize()}${variantOutput.name.capitalize()}",
                            type: SentryUploadProguardMappingsTask) {
                        dependsOn(generateUuidTask)
                        workingDir project.rootDir
                        getCliExecutable().set(cli)
                        getSentryProperties().set(project.file(SentryPropertiesFileProvider.getPropertiesFilePath(project, variant)))
                        mappingsUuid.set(generateUuidTask.outputUuid)
                        getMappingsFile().set(mappingFile)
                        getAutoUpload().set(extension.autoUpload.get())
                        def buildTypeProperties = variant.buildType.ext
                        if (buildTypeProperties.has(SENTRY_ORG_PARAMETER)) {
                            getSentryOrganization().set(buildTypeProperties.get(SENTRY_ORG_PARAMETER).toString())
                        }
                        if (buildTypeProperties.has(SENTRY_PROJECT_PARAMETER)) {
                            getSentryProject().set(buildTypeProperties.get(SENTRY_PROJECT_PARAMETER).toString())
                        }
                    }

                    variant.register(uploadSentryProguardMappingsTask)
                    project.android.sourceSets[variant.name].assets.srcDir(generateUuidTask.outputDirectory)

                    // create and hooks the uploading of native symbols task after the assembling task
                    def variantOutputName = "${variant.name.capitalize()}${variantOutput.name.capitalize()}"
                    def uploadNativeSymbolsTask = project.tasks.create(
                            name: "uploadNativeSymbolsFor${variantOutputName}",
                            type: SentryUploadNativeSymbolsTask) {
                        workingDir project.rootDir
                        getCliExecutable().set(cli)
                        getSentryProperties().set(project.file(SentryPropertiesFileProvider.getPropertiesFilePath(project, variant)))
                        getIncludeNativeSources().set(extension.includeNativeSources.get())
                        getVariantName().set(variant.name)
                        def buildTypeProperties = variant.buildType.ext
                        if (buildTypeProperties.has(SENTRY_ORG_PARAMETER)) {
                            getSentryOrganization().set(buildTypeProperties.get(SENTRY_ORG_PARAMETER).toString())
                        }
                        if (buildTypeProperties.has(SENTRY_PROJECT_PARAMETER)) {
                            getSentryProject().set(buildTypeProperties.get(SENTRY_PROJECT_PARAMETER).toString())
                        }
                    }

                    // and run before dex transformation.  If we managed to find the dex task
                    // we set ourselves as dependency, otherwise we just hack outselves into
                    // the proguard task's doLast.
                    if (dexTask != null) {
                        dexTask.dependsOn uploadSentryProguardMappingsTask
                    }

                    if (transformerTask != null) {
                        transformerTask.finalizedBy uploadSentryProguardMappingsTask
                    }

                    // To include proguard uuid file into aab, run before bundle task.
                    if (preBundleTask != null) {
                        preBundleTask.dependsOn uploadSentryProguardMappingsTask
                    }

                    // find the package task
                    def packageTask = SentryTasksProvider.getPackageTask(project, variant.name)
                    if (packageTask != null) {
                        project.logger.info("packageTask ${packageTask.path}")
                    } else {
                        project.logger.info("packageTask is null")
                    }

                    // the package task will only be executed if the uploadSentryProguardMappingsTask has already been executed.
                    if (packageTask != null) {
                        packageTask.dependsOn uploadSentryProguardMappingsTask
                    }

                    // find the assemble task
                    def assembleTask = SentryTasksProvider.getAssembleTask(variant)
                    if (assembleTask != null) {
                        project.logger.info("assembleTask ${assembleTask.path}")
                    } else {
                        project.logger.info("assembleTask is null")
                    }

                    // uploadNativeSymbolsTask only will be executed after the assemble task
                    // and also only if uploadNativeSymbols is enabled, this is opt-in feature
                    if (assembleTask != null) {
                        if (extension.uploadNativeSymbols.get()) {
                            assembleTask.finalizedBy uploadNativeSymbolsTask

                            // if its a bundle aab, assemble might not be executed, so we hook into bundle task
                            if (bundleTask != null) {
                                bundleTask.finalizedBy uploadNativeSymbolsTask
                            }
                        } else {
                            project.logger.info("uploadNativeSymbolsTask won't be executed")
                        }
                    }
                }
            }
        }
    }
}
