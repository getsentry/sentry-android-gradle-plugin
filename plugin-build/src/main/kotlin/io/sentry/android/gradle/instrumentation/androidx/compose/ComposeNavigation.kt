package io.sentry.android.gradle.instrumentation.androidx.compose

import io.sentry.android.gradle.instrumentation.util.MethodReplacingInstrumentable
import org.objectweb.asm.Opcodes

/* ktlint-disable max-line-length */
class ComposeNavigation : MethodReplacingInstrumentable(
    listOf(
        MethodReplacement(
            Opcodes.INVOKESTATIC,
            "androidx/navigation/compose/NavHostControllerKt",
            "rememberNavController",
            "([Landroidx/navigation/Navigator;Landroidx/compose/runtime/Composer;I)Landroidx/navigation/NavHostController;",
            "io/sentry/compose/SentryNavigationIntegrationKt",
            "rememberNavController"
        )
    )
)
