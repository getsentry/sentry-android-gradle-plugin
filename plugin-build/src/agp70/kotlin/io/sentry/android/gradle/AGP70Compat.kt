@file:Suppress("UnstableApiUsage")

package io.sentry.android.gradle

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.Variant

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
