package io.sentry.android.gradle.snapshot.preview

/** Mutable config populated by [PreviewAnnotationVisitor] during ASM scanning. */
internal data class PreviewConfig(
  var name: String? = null,
  var group: String? = null,
  var uiMode: Int? = null,
  var locale: String? = null,
  var fontScale: Float? = null,
  var heightDp: Int? = null,
  var widthDp: Int? = null,
  var showBackground: Boolean? = null,
  var backgroundColor: Long? = null,
  var showSystemUi: Boolean? = null,
  var device: String? = null,
  var apiLevel: Int? = null,
  var wallpaper: Int? = null,
)

/** Output config written to JSON and consumed by the generated test. */
internal data class PreviewSnapshotConfig(
  val displayName: String,
  val className: String,
  val methodName: String,
)

/** Metadata for a custom multi-preview annotation discovered in the first ASM pass. */
internal data class CustomPreviewAnnotation(
  val previewConfigs: MutableList<PreviewConfig> = mutableListOf()
)
