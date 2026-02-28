package io.sentry.android.gradle.snapshot.preview

internal const val PREVIEW_ANNOTATION_DESC = "Landroidx/compose/ui/tooling/preview/Preview;"

internal const val PREVIEW_CONTAINER_ANNOTATION_DESC =
  "Landroidx/compose/ui/tooling/preview/Preview\$Container;"

internal const val PREVIEW_LIGHT_DARK_ANNOTATION_DESC =
  "Landroidx/compose/ui/tooling/preview/PreviewLightDark;"

internal const val PREVIEW_FONT_SCALE_ANNOTATION_DESC =
  "Landroidx/compose/ui/tooling/preview/PreviewFontScale;"

internal const val PREVIEW_SCREEN_SIZES_ANNOTATION_DESC =
  "Landroidx/compose/ui/tooling/preview/PreviewScreenSizes;"

internal const val PREVIEW_DYNAMIC_COLORS_ANNOTATION_DESC =
  "Landroidx/compose/ui/tooling/preview/PreviewDynamicColors;"

internal const val PREVIEW_WEAR_SMALL_ROUND_ANNOTATION_DESC =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewSmallRound;"

internal const val PREVIEW_WEAR_LARGE_ROUND_ANNOTATION_DESC =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewLargeRound;"

internal const val PREVIEW_WEAR_SQUARE_ANNOTATION_DESC =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewSquare;"

internal const val PREVIEW_WEAR_DEVICES_ANNOTATION_DESC =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewDevices;"

internal const val PREVIEW_WEAR_FONT_SCALES =
  "Landroidx/wear/compose/ui/tooling/preview/WearPreviewFontScales;"

// https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/content/res/Configuration.java
internal const val UI_MODE_NIGHT_YES: Int = 32
internal const val UI_MODE_TYPE_NORMAL: Int = 1

// https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-tooling-preview/src/androidMain/kotlin/androidx/compose/ui/tooling/preview/Device.android.kt
internal const val PHONE =
  "spec:id=reference_phone,shape=Normal,width=411,height=891,unit=dp,dpi=420"
internal const val FOLDABLE =
  "spec:id=reference_foldable,shape=Normal,width=673,height=841,unit=dp,dpi=420"
internal const val TABLET =
  "spec:id=reference_tablet,shape=Normal,width=1280,height=800,unit=dp,dpi=240"
internal const val DESKTOP =
  "spec:id=reference_desktop,shape=Normal,width=1920,height=1080,unit=dp,dpi=160"

// https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:wear/wear-tooling-preview/src/main/java/androidx/wear/tooling/preview/devices/WearDevice.kt
internal const val LARGE_ROUND = "id:wearos_large_round"
internal const val SMALL_ROUND = "id:wearos_small_round"
internal const val SQUARE = "id:wearos_square"
