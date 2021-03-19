package io.sentry.android.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.Version
import io.sentry.android.gradle.tasks.SentryGenerateProguardUuidTask
import io.sentry.android.gradle.tasks.SentryUploadProguardMappingsTask
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.util.VersionNumber

class SentryPlugin implements Plugin<Project> {
    static final String GROUP_NAME = 'Sentry'
    private static final String SENTRY_ORG_PARAMETER = "sentryOrg"
    private static final String SENTRY_PROJECT_PARAMETER = "sentryProject"

    /**
     * Returns the transformer task for the given project and variant.
     * It could be either ProGuard or R8
     *
     * @param project the given project
     * @param variant the given variant
     * @return the task or null otherwise
     */
    static Task getTransformerTask(Project project, ApplicationVariant variant) {
        def names = [
                // Android Studio 3.3 includes the R8 shrinker.
                "transformClassesAndResourcesWithR8For${variant.name.capitalize()}",
                "transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}",
                "minify${variant.name.capitalize()}WithR8",
                "minify${variant.name.capitalize()}WithProguard"
        ]

        return names.findResult { project.tasks.findByName(it) } ?: project.tasks.findByName("proguard${names[1]}")
    }

    /**
     * Returns the dex task for the given project and variant.
     *
     * @param project
     * @param variant
     * @return
     */
    static Task getDexTask(Project project, ApplicationVariant variant) {
        def names = [
                "transformClassesWithDexFor${variant.name.capitalize()}",
                "transformClassesWithDexBuilderFor${variant.name.capitalize()}",
                "transformClassesAndDexWithShrinkResFor${variant.name.capitalize()}"
        ]

        return names.findResult { project.tasks.findByName(it) } ?: project.tasks.findByName("dex${names[0]}")
    }

    /**
     * Returns the pre bundle task for the given project and variant.
     *
     * @param project
     * @param variant
     * @return
     */
    static Task getPreBundleTask(Project project, ApplicationVariant variant) {
        return project.tasks.findByName("build${variant.name.capitalize()}PreBundle")
    }

    /**
     * Returns the pre bundle task for the given project and variant.
     *
     * @param project
     * @param variant
     * @return
     */
    static Task getBundleTask(Project project, ApplicationVariant variant) {
        return project.tasks.findByName("bundle${variant.name.capitalize()}")
    }

    void apply(Project project) {
        SentryPluginExtension extension = project.extensions.create("sentry", SentryPluginExtension, project)

        project.afterEvaluate {
            if (!project.plugins.hasPlugin(AppPlugin) && !project.getPlugins().hasPlugin(LibraryPlugin)) {
                throw new IllegalStateException('Must apply \'com.android.application\' first!')
            }

            project.android.applicationVariants.all { ApplicationVariant variant ->
                variant.outputs.each { variantOutput ->

                    def mappingFile = getMappingFile(variant, project)
                    def transformerTask = getTransformerTask(project, variant)

                    def dexTask = getDexTask(project, variant)
                    if (dexTask != null) {
                        project.logger.info("dexTask ${dexTask.path}")
                    } else {
                        project.logger.info("dexTask is null")
                    }

                    def preBundleTask = getPreBundleTask(project, variant)
                    if (preBundleTask != null) {
                        project.logger.info("preBundleTask ${preBundleTask.path}")
                    } else {
                        project.logger.info("preBundleTask is null")
                    }

                    def bundleTask = getBundleTask(project, variant)
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
                        getSentryProperties().set(project.file(getPropsString(project, variant)))
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
                    def uploadNativeSymbolsTaskName = "uploadNativeSymbolsFor${variantOutputName}"
                    def uploadNativeSymbolsTask = project.tasks.create(
                            name: uploadNativeSymbolsTaskName,
                            type: Exec) {
                        description "Uploads Native symbols."
                        workingDir project.rootDir

                        def propsFile = getPropsString(project, variant)

                        if (propsFile != null) {
                            environment("SENTRY_PROPERTIES", propsFile)
                        } else {
                            project.logger.info("propsFile is null")
                        }

                        def nativeArgs = [
                                cli,
                                "upload-dif",
                        ]

                        def buildTypeProperties = variant.buildType.ext
                        if (buildTypeProperties.has(SENTRY_ORG_PARAMETER)) {
                            nativeArgs.add("-o")
                            nativeArgs.add(buildTypeProperties.get(SENTRY_ORG_PARAMETER).toString())
                        }
                        if (buildTypeProperties.has(SENTRY_PROJECT_PARAMETER)) {
                            nativeArgs.add("-p")
                            nativeArgs.add(buildTypeProperties.get(SENTRY_PROJECT_PARAMETER).toString())
                        }

                        // eg absoluteProjectFolderPath/build/intermediates/merged_native_libs/{variant} where {variant} could be debug/release...
                        def symbolsPath = "${project.projectDir}${File.separator}build${File.separator}intermediates${File.separator}merged_native_libs${File.separator}${variant.name}"
                        project.logger.info("symbolsPath: ${symbolsPath}")

                        nativeArgs.add("${symbolsPath}")

                        // only include sources if includeNativeSources is enabled, this is opt-in feature
                        if (extension.includeNativeSources.get()) {
                            nativeArgs.add("--include-sources")
                        }

                        project.logger.info("nativeArgs args: ${nativeArgs.toString()}")

                        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                            commandLine("cmd", "/c", *nativeArgs)
                        } else {
                            commandLine(*nativeArgs)
                        }

                        project.logger.info("nativeArgs executed.")

                        enabled true
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
                    def packageTask = getPackageTask(project, variant)
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
                    def assembleTask = findAssembleTask(variant, project)
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

    /**
     * Returns the assemble task
     * @param project the given project
     * @param variant the given variant
     * @return the task if found or null otherwise
     */
    static Task findAssembleTask(ApplicationVariant variant, Project project) {
        try {
            return variant.assembleProvider.get()
        } catch (Exception ignored) {
            project.logger.error("findAssembleTask(): ${ignored.getMessage()}")
            return variant.assemble
        }
    }

    /**
     * Returns the GString with the current read'ed properties
     * @param project the given project
     * @param variant the given variant
     * @return the GString if found or null otherwise
     */
    static GString getPropsString(Project project, ApplicationVariant variant) {
        def buildTypeName = variant.buildType.name
        def flavorName = variant.flavorName
        // When flavor is used in combination with dimensions, variant.flavorName will be a concatenation
        // of flavors of different dimensions
        def propName = "sentry.properties"
        // current flavor name takes priority
        def possibleProps = []
        variant.productFlavors.each {
            // flavors used with dimension come in second
            possibleProps.push("${project.projectDir}/src/${it.name}/${propName}")
        }

        possibleProps = [
                "${project.projectDir}/src/${buildTypeName}/${propName}",
                "${project.projectDir}/src/${buildTypeName}/${flavorName}/${propName}",
                "${project.projectDir}/src/${flavorName}/${buildTypeName}/${propName}",
                "${project.projectDir}/src/${flavorName}/${propName}",
                "${project.projectDir}/${propName}",
                "${project.rootDir.toPath()}/src/${flavorName}/${propName}",
        ] + possibleProps + [
                "${project.rootDir.toPath()}/src/${buildTypeName}/${propName}",
                "${project.rootDir.toPath()}/src/${buildTypeName}/${flavorName}/${propName}",
                "${project.rootDir.toPath()}/src/${flavorName}/${buildTypeName}/${propName}",
                // Root sentry.properties is the last to be looked up
                "${project.rootDir.toPath()}/${propName}"
        ]

        def propsFile = null
        possibleProps.each {
            project.logger.info("Looking for Sentry properties at: $it")
            if (propsFile == null && new File(it).isFile()) {
                propsFile = it
                project.logger.info("Found Sentry properties in: $it")
            }
        }

        return propsFile
    }

    /**
     * Returns the package task
     * @param project the given project
     * @param variant the given variant
     * @return the package task or null if not found
     */
    static Task getPackageTask(Project project, ApplicationVariant variant) {
        def names = [
                "package${variant.name.capitalize()}",
                "package${variant.name.capitalize()}Bundle"
        ]

        return names.findResult { project.tasks.findByName(it) }
    }

    /**
     * Returns the mapping file
     * @param variant the ApplicationVariant
     * @return the file or null if not found
     */
    static File getMappingFile(ApplicationVariant variant, Project project) {
        try {
            def files = variant.getMappingFileProvider().get().files
            if (files.isEmpty()) {
                project.logger.debug("mappingFileProvider.files is empty for ${variant.name}")
                return null
            }
            project.logger.info("mapping files size: ${files.size()} for ${variant.name}")
            def file = files.iterator().next()
            project.logger.info("mapping file: ${file.path} for ${variant.name}")
            return file
        } catch (Exception ignored) {
            project.logger.error("getMappingFile(): ${ignored.getMessage()}")
            return variant.getMappingFile()
        }
    }
}
