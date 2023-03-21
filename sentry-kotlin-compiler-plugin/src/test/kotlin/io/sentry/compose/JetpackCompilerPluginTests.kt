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
    fun `test basic example`() {
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt", contents = """
            package io.sentry.compose.examples
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.text.BasicText
            class Example {
                @Composable
                fun NoModifier() {
                    BasicText(
                        text = "Hello World 0"
                    )
                }
            }
            """.trimIndent()
        )
        compile(kotlinSource)
    }

    @Test
    fun `test basic 2 example`() {
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt", contents = """
            package io.sentry.compose.examples
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.text.BasicText
            import androidx.compose.ui.Modifier
            class Example {
                @Composable
                fun ExistingModifier() {
                    val x = Modifier
                    BasicText(
                        text = "Hello World 0",
                        modifier = x
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
