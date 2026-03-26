package io.sentry.android.gradle.snapshot.metadata

// region Annotation descriptors

internal const val PREVIEW_DESCRIPTOR = "Landroidx/compose/ui/tooling/preview/Preview;"
internal const val PREVIEW_CONTAINER_DESCRIPTOR =
  "Landroidx/compose/ui/tooling/preview/Preview\$Container;"
internal const val PREVIEW_PARAMETER_DESCRIPTOR =
  "Landroidx/compose/ui/tooling/preview/PreviewParameter;"

private const val PREVIEW_LIGHT_DARK = "Landroidx/compose/ui/tooling/preview/PreviewLightDark;"
private const val PREVIEW_FONT_SCALE = "Landroidx/compose/ui/tooling/preview/PreviewFontScale;"
private const val PREVIEW_SCREEN_SIZES = "Landroidx/compose/ui/tooling/preview/PreviewScreenSizes;"
private const val PREVIEW_DYNAMIC_COLORS =
  "Landroidx/compose/ui/tooling/preview/PreviewDynamicColors;"

private const val WEAR_PREVIEW_SMALL_ROUND =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewSmallRound;"
private const val WEAR_PREVIEW_LARGE_ROUND =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewLargeRound;"
private const val WEAR_PREVIEW_SQUARE =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewSquare;"
private const val WEAR_PREVIEW_FONT_SCALES =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewFontScales;"
private const val WEAR_PREVIEW_DEVICES =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewDevices;"

// endregion

// region Device spec constants
// https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-tooling-preview/src/androidMain/kotlin/androidx/compose/ui/tooling/preview/Device.android.kt

private const val PHONE =
  "spec:id=reference_phone,shape=Normal,width=411,height=891,unit=dp,dpi=420"
private const val FOLDABLE =
  "spec:id=reference_foldable,shape=Normal,width=673,height=841,unit=dp,dpi=420"
private const val TABLET =
  "spec:id=reference_tablet,shape=Normal,width=1280,height=800,unit=dp,dpi=240"
private const val DESKTOP =
  "spec:id=reference_desktop,shape=Normal,width=1920,height=1080,unit=dp,dpi=160"
private const val SMALL_ROUND = "id:wearos_small_round"
private const val LARGE_ROUND = "id:wearos_large_round"
private const val SQUARE = "id:wearos_square"

// endregion

// region UI mode constants
// https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/content/res/Configuration.java

private const val UI_MODE_NIGHT_YES: Int = 32
private const val UI_MODE_TYPE_NORMAL: Int = 1

// endregion

// region Built-in multipreview expansion

@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun builtinMultipreviewConfigs(descriptor: String?): List<PreviewConfig>? {
  return when (descriptor) {
    PREVIEW_LIGHT_DARK ->
      listOf(
        PreviewConfig(name = "Light"),
        PreviewConfig(name = "Dark", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL),
      )

    PREVIEW_FONT_SCALE ->
      listOf(
        PreviewConfig(name = "85%", fontScale = 0.85f),
        PreviewConfig(name = "100%", fontScale = 1.0f),
        PreviewConfig(name = "115%", fontScale = 1.15f),
        PreviewConfig(name = "130%", fontScale = 1.3f),
        PreviewConfig(name = "150%", fontScale = 1.5f),
        PreviewConfig(name = "180%", fontScale = 1.8f),
        PreviewConfig(name = "200%", fontScale = 2f),
      )

    PREVIEW_SCREEN_SIZES ->
      listOf(
        PreviewConfig(name = "Phone", device = PHONE, showSystemUi = true),
        PreviewConfig(
          name = "Phone - Landscape",
          device = "spec:width = 411dp, height = 891dp, orientation = landscape, dpi = 420",
          showSystemUi = true,
        ),
        PreviewConfig(name = "Unfolded Foldable", device = FOLDABLE, showSystemUi = true),
        PreviewConfig(name = "Tablet", device = TABLET, showSystemUi = true),
        PreviewConfig(name = "Desktop", device = DESKTOP, showSystemUi = true),
      )

    PREVIEW_DYNAMIC_COLORS ->
      listOf(
        PreviewConfig(name = "Red", wallpaper = 0),
        PreviewConfig(name = "Green", wallpaper = 1),
        PreviewConfig(name = "Blue", wallpaper = 2),
        PreviewConfig(name = "Yellow", wallpaper = 3),
      )

    WEAR_PREVIEW_SMALL_ROUND ->
      listOf(
        PreviewConfig(
          device = SMALL_ROUND,
          backgroundColor = 0xff000000,
          showBackground = true,
          group = "Devices - Small Round",
          showSystemUi = true,
        )
      )

    WEAR_PREVIEW_LARGE_ROUND ->
      listOf(
        PreviewConfig(
          device = LARGE_ROUND,
          backgroundColor = 0xff000000,
          showBackground = true,
          group = "Devices - Large Round",
          showSystemUi = true,
        )
      )

    WEAR_PREVIEW_SQUARE ->
      listOf(
        PreviewConfig(
          device = SQUARE,
          backgroundColor = 0xff000000,
          showBackground = true,
          group = "Devices - Square",
          showSystemUi = true,
        )
      )

    WEAR_PREVIEW_FONT_SCALES -> {
      val base =
        PreviewConfig(
          device = SMALL_ROUND,
          showSystemUi = true,
          backgroundColor = 0xff000000,
          showBackground = true,
        )
      listOf(
        base.copy(group = "Fonts - Small", fontScale = 0.94f),
        base.copy(group = "Fonts - Normal", fontScale = 1f),
        base.copy(group = "Fonts - Medium", fontScale = 1.06f),
        base.copy(group = "Fonts - Large", fontScale = 1.12f),
        base.copy(group = "Fonts - Larger", fontScale = 1.18f),
        base.copy(group = "Fonts - Largest", fontScale = 1.24f),
      )
    }

    WEAR_PREVIEW_DEVICES -> {
      val base =
        PreviewConfig(showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
      listOf(
        base.copy(device = SMALL_ROUND, group = "Devices - Small Round"),
        base.copy(device = LARGE_ROUND, group = "Devices - Large Round"),
        base.copy(device = SQUARE, group = "Devices - Small Square"),
      )
    }

    else -> null
  }
}

// endregion
