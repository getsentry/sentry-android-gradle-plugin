package io.sentry.android.gradle.snapshot

import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.BaseExtension
import io.sentry.android.gradle.SentryTasksProvider.capitalized
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

@CacheableTask
abstract class GenerateSnapshotTestsTask : DefaultTask() {

  init {
    description =
      "Generates a parameterized Paparazzi test that snapshots all Compose @Preview composables"
  }

  @get:Input abstract val includePrivatePreviews: Property<Boolean>

  @get:Input abstract val packageTrees: ListProperty<String>

  @get:OutputDirectory abstract val outputDir: DirectoryProperty

  @TaskAction
  fun generate() {
    val outDir = outputDir.get().asFile
    if (outDir.exists()) {
      outDir.deleteRecursively()
    }

    val packageDir = File(outDir, PACKAGE_NAME.replace('.', '/'))
    packageDir.mkdirs()

    val content =
      generateTestFileContent(
        includePrivatePreviews = includePrivatePreviews.get(),
        packageTrees = packageTrees.get(),
      )
    File(packageDir, "$CLASS_NAME.kt").writeText(content)
    logger.lifecycle("Generated snapshot test: ${packageDir.absolutePath}/$CLASS_NAME.kt")
  }

  companion object {
    private const val PACKAGE_NAME = "io.sentry.snapshot"
    private const val CLASS_NAME = "ComposablePreviewSnapshotTest"

    fun register(
      project: Project,
      extension: SentrySnapshotExtension,
      android: BaseExtension,
      variant: ApplicationVariant,
    ): TaskProvider<GenerateSnapshotTestsTask> {
      return project.tasks.register(
        "generateSentrySnapshotTests${variant.name.capitalized}",
        GenerateSnapshotTestsTask::class.java,
      ) { task ->
        task.includePrivatePreviews.set(extension.includePrivatePreviews)
        // Fall back to the Android namespace when the user doesn't configure packageTrees
        task.packageTrees.set(
          extension.packageTrees.map { packages ->
            packages.ifEmpty { listOf(android.namespace!!) }
          }
        )
        task.outputDir.set(
          project.layout.buildDirectory.dir("generated/sentry/snapshotTests/${variant.name}")
        )
      }
    }

    @Suppress("LongMethod")
    private fun generateTestFileContent(
      includePrivatePreviews: Boolean,
      packageTrees: List<String>,
    ): String {
      val includePrivateExpr =
        if (includePrivatePreviews) "\n                .includePrivatePreviews()" else ""

      val packages = packageTrees.joinToString(", ") { "\"$it\"" }
      val scanExpr = ".scanPackageTrees($packages)"

      return """
package $PACKAGE_NAME

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.HtmlReportWriter
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.SnapshotVerifier
import app.cash.paparazzi.TestName
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.*
import kotlin.math.ceil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.device.DevicePreviewInfoParser
import sergio.sastre.composable.preview.scanner.android.device.domain.Device
import sergio.sastre.composable.preview.scanner.android.device.types.DEFAULT
import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

private class Dimensions(
    val screenWidthInPx: Int,
    val screenHeightInPx: Int,
)

private object ScreenDimensions {
    fun dimensions(
        parsedDevice: Device,
        widthDp: Int,
        heightDp: Int,
    ): Dimensions {
        val conversionFactor = parsedDevice.densityDpi / 160f
        val previewWidthInPx = ceil(widthDp * conversionFactor).toInt()
        val previewHeightInPx = ceil(heightDp * conversionFactor).toInt()
        return Dimensions(
            screenWidthInPx = when (widthDp > 0) {
                true -> previewWidthInPx
                false -> parsedDevice.dimensions.width.toInt()
            },
            screenHeightInPx = when (heightDp > 0) {
                true -> previewHeightInPx
                false -> parsedDevice.dimensions.height.toInt()
            },
        )
    }
}

private object DeviceConfigBuilder {
    fun build(preview: AndroidPreviewInfo): DeviceConfig {
        val parsedDevice =
            DevicePreviewInfoParser.parse(preview.device)?.inPx() ?: return DeviceConfig()

        val dimensions = ScreenDimensions.dimensions(
            parsedDevice = parsedDevice,
            widthDp = preview.widthDp,
            heightDp = preview.heightDp,
        )

        return DeviceConfig(
            screenHeight = dimensions.screenHeightInPx,
            screenWidth = dimensions.screenWidthInPx,
            density = Density(parsedDevice.densityDpi),
            xdpi = parsedDevice.densityDpi,
            ydpi = parsedDevice.densityDpi,
            size = ScreenSize.valueOf(parsedDevice.screenSize.name),
            ratio = ScreenRatio.valueOf(parsedDevice.screenRatio.name),
            screenRound = ScreenRound.valueOf(parsedDevice.shape.name),
            orientation = ScreenOrientation.valueOf(parsedDevice.orientation.name),
            locale = preview.locale.ifBlank { "en" },
            fontScale = preview.fontScale,
            nightMode = when (preview.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES) {
                true -> NightMode.NIGHT
                false -> NightMode.NOTNIGHT
            },
        )
    }
}

private val paparazziTestName =
    TestName(packageName = "Paparazzi", className = "Preview", methodName = "Test")

private class TestNameOverrideHandler(
    private val delegate: SnapshotHandler,
) : SnapshotHandler {
    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int,
    ): SnapshotHandler.FrameHandler {
        val newSnapshot = Snapshot(
            name = snapshot.name,
            testName = paparazziTestName,
            timestamp = snapshot.timestamp,
            tags = snapshot.tags,
            file = snapshot.file,
        )
        return delegate.newFrameHandler(newSnapshot, frameCount, fps)
    }

    override fun close() {
        delegate.close()
    }
}

private object PaparazziPreviewRule {
    const val UNDEFINED_API_LEVEL = -1
    const val MAX_API_LEVEL = 36

    fun createFor(preview: ComposablePreview<AndroidPreviewInfo>): Paparazzi {
        val previewInfo = preview.previewInfo
        val previewApiLevel = when (previewInfo.apiLevel == UNDEFINED_API_LEVEL) {
            true -> MAX_API_LEVEL
            false -> previewInfo.apiLevel
        }
        val tolerance = 0.0
        return Paparazzi(
            environment = detectEnvironment().copy(compileSdkVersion = previewApiLevel),
            deviceConfig = DeviceConfigBuilder.build(preview.previewInfo),
            supportsRtl = true,
            showSystemUi = previewInfo.showSystemUi,
            renderingMode = when {
                previewInfo.showSystemUi -> SessionParams.RenderingMode.NORMAL
                previewInfo.widthDp > 0 && previewInfo.heightDp > 0 -> SessionParams.RenderingMode.FULL_EXPAND
                else -> SessionParams.RenderingMode.SHRINK
            },
            snapshotHandler = TestNameOverrideHandler(
                when (System.getProperty("paparazzi.test.verify")?.toBoolean() == true) {
                    true -> SnapshotVerifier(maxPercentDifference = tolerance)
                    false -> HtmlReportWriter(maxPercentDifference = tolerance)
                }
            ),
            maxPercentDifference = tolerance,
        )
    }
}

@Composable
private fun SystemUiSize(
    widthInDp: Int,
    heightInDp: Int,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(width = widthInDp.dp, height = heightInDp.dp)
            .background(Color.White)
    ) {
        content()
    }
}

@Composable
private fun PreviewBackground(
    showBackground: Boolean,
    backgroundColor: Long,
    content: @Composable () -> Unit,
) {
    when (showBackground) {
        false -> content()
        true -> {
            val color = when (backgroundColor != 0L) {
                true -> Color(backgroundColor)
                false -> Color.White
            }
            Box(Modifier.background(color)) {
                content()
            }
        }
    }
}

/**
 * Auto-generated by Sentry Snapshot Plugin.
 */
@RunWith(Parameterized::class)
class $CLASS_NAME(
    private val preview: ComposablePreview<AndroidPreviewInfo>,
) {

    companion object {
        private val cachedPreviews: List<ComposablePreview<AndroidPreviewInfo>> by lazy {
            AndroidComposablePreviewScanner()
                $scanExpr$includePrivateExpr
                .getPreviews()
        }

        @JvmStatic
        @Parameterized.Parameters
        fun values(): List<ComposablePreview<AndroidPreviewInfo>> = cachedPreviews
    }

    @get:Rule
    val paparazzi: Paparazzi = PaparazziPreviewRule.createFor(preview)

    @Test
    fun snapshot() {
        val screenshotId = AndroidPreviewScreenshotIdBuilder(preview)
            .doNotIgnoreMethodParametersType()
            .encodeUnsafeCharacters()
            .build()

        paparazzi.snapshot(name = screenshotId) {
            val previewInfo = preview.previewInfo
            when (previewInfo.showSystemUi) {
                false -> PreviewBackground(
                    showBackground = previewInfo.showBackground,
                    backgroundColor = previewInfo.backgroundColor,
                ) {
                    preview()
                }

                true -> {
                    val parsedDevice = (DevicePreviewInfoParser.parse(previewInfo.device) ?: DEFAULT).inDp()
                    SystemUiSize(
                        widthInDp = parsedDevice.dimensions.width.toInt(),
                        heightInDp = parsedDevice.dimensions.height.toInt(),
                    ) {
                        PreviewBackground(
                            showBackground = true,
                            backgroundColor = previewInfo.backgroundColor,
                        ) {
                            preview()
                        }
                    }
                }
            }
        }
    }
}
"""
        .trimStart()
    }
  }
}
