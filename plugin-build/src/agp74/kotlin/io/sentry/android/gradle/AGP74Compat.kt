@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.CanMinifyCode
import com.android.build.api.variant.Variant
import io.sentry.gradle.common.AndroidVariant
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

data class AndroidVariant74(
    private val variant: Variant
) : AndroidVariant {
    override val name: String = variant.name
    override val flavorName: String? = variant.flavorName
    override val buildTypeName: String? = variant.buildType
    override val productFlavors: List<String> = variant.productFlavors.map { it.second }
    override val isMinifyEnabled: Boolean = (variant as? CanMinifyCode)?.isMinifyEnabled == true
    override fun mappingFileProvider(project: Project): Provider<FileCollection> =
        project.provider {
            project.files(variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE))
        }
}

fun <T : Task> configureGeneratedSourcesFor74(
    variant: Variant,
    vararg tasks: Pair<TaskProvider<out T>, (T) -> DirectoryProperty>
) {
    tasks.forEach { (task, output) ->
        variant.sources.assets?.addGeneratedSourceDirectory(task, output)
    }
}

fun <T : InstrumentationParameters> configureInstrumentationFor74(
    variant: Variant,
    classVisitorFactoryImplClass: Class<out AsmClassVisitorFactory<T>>,
    scope: InstrumentationScope,
    mode: FramesComputationMode,
    instrumentationParamsConfig: (T) -> Unit
) {
    variant.instrumentation.transformClassesWith(
        classVisitorFactoryImplClass,
        scope,
        instrumentationParamsConfig
    )
    variant.instrumentation.setAsmFramesComputationMode(mode)
}
