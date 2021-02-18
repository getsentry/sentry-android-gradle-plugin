package io.sentry.android.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.tasks.MergeNativeLibsTask
import io.sentry.android.gradle.UnpackSentryCliTask.Companion.tryRegisterUnpackCliTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale
import java.util.Properties

@Suppress("UnstableApiUsage")
class SentryPlugin : Plugin<Project> {
    private lateinit var extension: SentryExtension
    private var unpackSentryCliTask: TaskProvider<UnpackSentryCliTask>? = null

    @OptIn(ExperimentalStdlibApi::class)
    override fun apply(target: Project) {
        extension = target.extensions.create("sentry", SentryExtension::class.java)

        unpackSentryCliTask = target.tryRegisterUnpackCliTask()

        target.pluginManager.withPlugin("com.android.library") { wrongAndroidPlugin("library") }
        target.pluginManager.withPlugin("com.android.dynamic-feature") { wrongAndroidPlugin("dynamic feature") }
        target.pluginManager.withPlugin("com.android.instant") { wrongAndroidPlugin("instant") }

        target.pluginManager.withPlugin("com.android.application") {
            val android = target.extensions.getByType(AppExtension::class.java)
            val generateSentryProguardSettings = target.registerSentryProguardConfigTask(android)

            android.buildTypes.configureEach {
                (it as ExtensionAware).extensions.create("sentry", SentryBuildTypeExtension::class.java)
            }

            android.applicationVariants.configureEach { variant ->
                val minifyTask = target.findMinifyTaskFor(variant)

                val buildTypeExtension = (android.buildTypes.getByName(variant.buildType.name) as ExtensionAware)
                    .extensions
                    .getByType(SentryBuildTypeExtension::class.java)

                val sentryPropertiesFile = target.findSentryPropertiesFile(variant)
                val sentryProperties = Properties().apply {
                    try {
                        sentryPropertiesFile?.inputStream()?.use(::load)
                    } catch (e: FileNotFoundException) {
                        target.logger.info("Could not find the sentry properties file", e)
                        // it's okay, we can ignore it.
                    }
                }

                val sentryCli = target.getSentryCliProvider(sentryProperties)

                minifyTask?.configure { it.dependsOn(generateSentryProguardSettings) }

                val enabled = extension.variantFilter.map { it.isSatisfiedBy(variant) }

                val generateUuidTask = target.tasks.register(
                    "generateSentryProguardUuid${variant.name.capitalize(Locale.ROOT)}",
                    GenerateSentryProguardUuidTask::class.java
                ) { task ->
                    task.outputDirectory.set(target.file("build/generated/assets/sentry/${variant.name}"))

                    task.doFirst {
                        it.logger.info("debugMetaPropPath: ${task.outputFile.get().asFile}")
                    }

                    task.onlyIf { enabled.get() }
                }

                variant.mergeAssetsProvider.configure { it.dependsOn(generateUuidTask) }

                // create a task that persists our proguard uuid as android asset
                if (minifyTask != null) {
                    val uploadSentryProguardMappingsTask = target.registerUploadTask(
                        "uploadSentryProguardMappings${variant.name.capitalize(Locale.ROOT)}",
                        SentryUploadProguardMappingsTask::class.java,
                        variant,
                        buildTypeExtension,
                        sentryPropertiesFile,
                        sentryCli
                    ) { task ->
                        task.dependsOn(generateUuidTask)
                        task.dependsOn(minifyTask)
                        task.mappingsUuid.set(generateUuidTask.flatMap { it.outputUuid })
                        task.mappingsFile.fileProvider(variant.mappingFileProvider.map {
                            if (it.isEmpty) null else it.singleFile
                        })
                        task.autoUpload.set(target.provider { buildTypeExtension.autoUpload.orNull ?: extension.autoUpload.get() })
                    }
                    // The task unfortunately has to be configured early, as soon as Android supports it the provider should
                    // be passed instead
                    variant.register(uploadSentryProguardMappingsTask.get())
                    android.sourceSets.getByName(variant.name).assets.srcDir(generateUuidTask.flatMap { it.outputDirectory })
                }

                val mergeNativeLibsTask = target.tasks.named("merge${variant.name.capitalize(Locale.ROOT)}NativeLibs", MergeNativeLibsTask::class.java)

                val uploadSentryNativeSymbols = target.registerUploadTask(
                    "uploadSentryNativeSymbols${variant.name.capitalize(Locale.ROOT)}",
                    SentryUploadNativeSymbolsTask::class.java,
                    variant,
                    buildTypeExtension,
                    sentryPropertiesFile,
                    sentryCli
                ) { task ->
                    task.dependsOn(mergeNativeLibsTask)
                    task.symbolsDirectory.set(mergeNativeLibsTask.flatMap { it.outputDir })
                    task.includeNativeSources.set(extension.includeNativeSources)
                    task.onlyIf {
                         buildTypeExtension.uploadNativeSymbols.orNull ?: extension.uploadNativeSymbols.get()
                    }
                }
                variant.register(uploadSentryNativeSymbols.get())
            }

        }

        target.afterEvaluate {
            if (!target.pluginManager.hasPlugin("com.android.application")) {
                throw GradleException("The com.android.application plugin was never added. The io.sentry.android.gradle plugin must be used in a module that also uses the com.android.application plugin")
            }
        }
    }

    private fun wrongAndroidPlugin(type: String): Nothing {
        throw GradleException("You cannot use the io.sentry.android.gradle in an Android $type module. The Sentry plugin should be applied your application module (the one that uses com.android.application)")
    }

    private fun Project.registerSentryProguardConfigTask(android: AppExtension): TaskProvider<SentryProguardConfigTask> {
        val outputFile = buildDir.resolve("intermediates/sentry/sentry.pro")
        android.defaultConfig.proguardFiles(outputFile)
        return tasks.register("generateSentryProguardSettings", SentryProguardConfigTask::class.java) { task ->
            task.outputFile.set(outputFile)
            task.generateFile.set(extension.autoProguardConfig)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun Project.findMinifyTaskFor(variant: ApplicationVariant): TaskProvider<Task>? =
        // Android Studio 3.3 includes the R8 shrinker.
        tasks.namedOrNull("minify${variant.name.capitalize(Locale.ROOT)}WithR8")
            ?: tasks.namedOrNull("minify${variant.name.capitalize(Locale.ROOT)}WithProguard")

    private fun TaskContainer.namedOrNull(name: String): TaskProvider<Task>? = if (name in names) named(name) else null

    private fun <T : SentryUploadTask> Project.registerUploadTask(
        name: String,
        taskType: Class<T>,
        variant: ApplicationVariant,
        buildTypeExtension: SentryBuildTypeExtension,
        sentryPropertiesFile: File?,
        sentryCli: Provider<String>,
        configure: (T) -> Unit
    ): TaskProvider<T> = tasks.register(name, taskType) { task ->
        task.workingDir(rootDir)
        task.cliExecutable.set(sentryCli)
        task.sentryPropertiesFile.set(sentryPropertiesFile)
        task.sentryOrganization.set(provider { buildTypeExtension.organization.orNull ?: extension.organization.orNull })
        task.sentryProject.set(provider { buildTypeExtension.project.orNull ?: extension.project.orNull })
        val variantEnabled = extension.variantFilter.map { it.isSatisfiedBy(variant) }
        task.onlyIf { variantEnabled.get() && buildTypeExtension.enabled.get() }
        configure(task)
    }

    private fun Project.findSentryPropertiesFile(variant: ApplicationVariant): File? =
        listOf(
            projectDir.resolve("src/${variant.name}/sentry.properties"),
            projectDir.resolve("src/${variant.buildType.name}/sentry.properties"),
            projectDir.resolve("sentry.properties"),
            rootProject.projectDir.resolve("sentry.properties")
        ).firstOrNull { it.exists() }

    /**
     * Attempts to get the path to the Sentry CLI. It has the following priority order:
     * 1. [SentryExtension.sentryCli]
     * 2. `cli.executable` in `sentry.properties`
     * 3. The sibling `node_modules` directory
     * 4. Using the bundled CLI via the [UnpackSentryCliTask] task.
     *
     * If no CLI is found, the `sentry-cli` from the system path will be used.
     */
    private fun Project.getSentryCliProvider(sentryProperties: Properties): Provider<String> =
        extension.sentryCli.map { it.asFile.absolutePath }
            .orElse(provider {
                sentryProperties.getProperty("cli.executable") ?: findNodeModulesCli()
            }.orElse((unpackSentryCliTask
                ?.flatMap { it.outputFile }
                ?.map { it.asFile.absolutePath }
                ?: provider<String> { null })
                .orElse("sentry-cli")
            )
            )

    private fun Project.findNodeModulesCli(): String? {
        // in case there is a version from npm right around the corner use that one. This
        // is the case for react-native-sentry for instance
        val possibleExePaths = listOf(
            "../node_modules/@sentry/cli/bin/sentry-cli",
            "../node_modules/sentry-cli-binary/bin/sentry-cli",
        )

        return possibleExePaths
            .plus(possibleExePaths.map { "$it.exe" })
            .map(rootProject::file)
            .firstOrNull { it.exists() }
            ?.absolutePath
    }
}