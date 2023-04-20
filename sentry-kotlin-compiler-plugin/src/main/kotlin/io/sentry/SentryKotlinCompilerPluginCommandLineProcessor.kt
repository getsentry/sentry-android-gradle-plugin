package io.sentry

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class SentryKotlinCompilerPluginCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = "io.sentry.sentry-kotlin-compiler-plugin"

    override val pluginOptions: Collection<CliOption> = emptyList()
}
