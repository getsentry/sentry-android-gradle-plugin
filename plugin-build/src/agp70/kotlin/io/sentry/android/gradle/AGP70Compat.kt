@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.Variant
import com.android.build.gradle.api.ApplicationVariant
import io.sentry.gradle.common.AndroidVariant
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

data class AndroidVariant70(
    private val variant: ApplicationVariant
) : AndroidVariant {
    override val name: String = variant.name
    override val flavorName: String? = variant.flavorName
    override val buildTypeName: String = variant.buildType.name
    override val productFlavors: List<String> = variant.productFlavors.map { it.name }
    override val isMinifyEnabled: Boolean = variant.buildType.isMinifyEnabled
    override val packageProvider: TaskProvider<out Task>? = variant.packageApplicationProvider
    override val assembleProvider: TaskProvider<out Task>? = variant.assembleProvider
    override fun mappingFileProvider(project: Project): Provider<FileCollection> =
        variant.mappingFileProvider
}

fun <T : InstrumentationParameters> configureInstrumentationFor70(
    variant: Variant,
    classVisitorFactoryImplClass: Class<out AsmClassVisitorFactory<T>>,
    scope: InstrumentationScope,
    mode: FramesComputationMode,
    instrumentationParamsConfig: (T) -> Unit
) {
    variant.transformClassesWith(
        classVisitorFactoryImplClass,
        scope,
        instrumentationParamsConfig
    )
    variant.setAsmFramesComputationMode(mode)
}
