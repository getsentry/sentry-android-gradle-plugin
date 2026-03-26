package io.sentry.android.gradle.snapshot.metadata

data class PreviewMetadataExport(val previews: List<PreviewMetadata>) {
  fun toMap(): Map<String, Any> = mapOf("previews" to previews.map { it.toMap() })
}

data class PreviewMetadata(
  val className: String,
  val methodName: String,
  val sourceFileName: String?,
  val previewName: String?,
  val configuration: PreviewConfiguration,
  val device: DeviceMetadata?,
  val previewParameter: PreviewParameterMetadata?,
) {
  fun toMap(): Map<String, Any?> =
    mapOf(
      "className" to className,
      "methodName" to methodName,
      "sourceFileName" to sourceFileName,
      "previewName" to previewName,
      "configuration" to configuration.toMap(),
      "device" to device?.toMap(),
      "previewParameter" to previewParameter?.toMap(),
    )
}

data class PreviewConfiguration(
  val apiLevel: Int?,
  val locale: String?,
  val fontScale: Float,
  val uiMode: Int,
  val showSystemUi: Boolean,
  val showBackground: Boolean,
  val backgroundColor: Long?,
  val group: String?,
  val wallpaper: Int?,
) {
  fun toMap(): Map<String, Any?> =
    mapOf(
      "apiLevel" to apiLevel,
      "locale" to locale,
      "fontScale" to fontScale,
      "uiMode" to uiMode,
      "showSystemUi" to showSystemUi,
      "showBackground" to showBackground,
      "backgroundColor" to backgroundColor,
      "group" to group,
      "wallpaper" to wallpaper,
    )
}

data class DeviceMetadata(val deviceSpec: String?, val widthDp: Int?, val heightDp: Int?) {
  fun toMap(): Map<String, Any?> =
    mapOf("deviceSpec" to deviceSpec, "widthDp" to widthDp, "heightDp" to heightDp)
}

data class PreviewParameterMetadata(
  val parameterName: String,
  val providerClassFqn: String,
  val limit: Int?,
  val index: Int?,
) {
  fun toMap(): Map<String, Any?> =
    mapOf(
      "parameterName" to parameterName,
      "providerClassFqn" to providerClassFqn,
      "limit" to limit,
      "index" to index,
    )
}
