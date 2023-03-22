package io.sentry.compose

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.sentry.SentryKotlinCompilerPlugin
import io.sentry.SentryKotlinCompilerPluginCommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class KotlinCompilerPluginComposeTest {

    private val fakeSentryModifier = SourceFile.kotlin(
        name = "Modifier.kt", contents = """
            package io.sentry.compose
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.platform.testTag
            class Modifier {
                companion object {
                    @JvmStatic
                    fun sentryModifier(tag: String) : Modifier = Modifier.testTag(tag)
                }
            }
            """.trimIndent()
    )

    @Test
    fun `sentry modifier is used when no modifier is present`() {
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt", contents = """
            package io.sentry.compose.examples
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.text.BasicText
            class Example {
                @Composable
                fun NoModifier() {
                    BasicText(
                        text = "No Modifier Argument"
                    )
                }
            }
            """.trimIndent()
        )
        compile(kotlinSource)
    }

    @Test
    fun `existing modifier companion is replaced with sentry modifier`() {
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt", contents = """
            package io.sentry.compose.examples

            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.foundation.layout.padding
            import androidx.compose.ui.unit.dp
            import androidx.compose.foundation.text.BasicText
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            class Example {
                @Composable
                fun ExistingModifier() {
                    BasicText(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        text = "Existing Modifier"
                    )
                }
            }
            """.trimIndent()
        )
        compile(kotlinSource)
    }

    @Test
    fun `modifier as param enriched with sentry modifier`() {
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt", contents = """
            package io.sentry.compose.examples

            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.foundation.layout.padding
            import androidx.compose.ui.unit.dp
            import androidx.compose.foundation.text.BasicText
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            class Example {
                @Composable
                fun ModifierAsParam(modifier : Modifier = Modifier.Companion) {
                    BasicText(
                        modifier = modifier.fillMaxSize().padding(8.dp),
                        text = "Existing Modifier"
                    )
                }
            }
            """.trimIndent()
        )
        compile(kotlinSource)
    }

    private fun compile(kotlinSource: SourceFile): KotlinCompilation.Result {
        return KotlinCompilation().apply {
            sources = listOf(fakeSentryModifier, kotlinSource)
            compilerPluginRegistrars = listOf(SentryKotlinCompilerPlugin())
            commandLineProcessors = listOf(SentryKotlinCompilerPluginCommandLineProcessor())
            inheritClassPath = true
            messageOutputStream = System.out // see diagnostics in real time
        }.compile()
    }
}
