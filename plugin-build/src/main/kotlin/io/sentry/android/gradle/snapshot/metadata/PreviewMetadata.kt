package io.sentry.android.gradle.snapshot.metadata

data class PreviewMetadataExport(
  val scannedPackages: List<String>,
  val previews: List<PreviewMetadata>,
) {
  fun toMap(): Map<String, Any> =
    mapOf("scannedPackages" to scannedPackages, "previews" to previews.map { it.toMap() })
}

data class PreviewMetadata(
  val className: String,
  val methodName: String,
  val sourceFileName: String?,
  val previewName: String?,
  val configuration: PreviewConfiguration,
  val device: DeviceMetadata?,
) {
  fun toMap(): Map<String, Any?> =
    mapOf(
      "className" to className,
      "methodName" to methodName,
      "sourceFileName" to sourceFileName,
      "previewName" to previewName,
      "configuration" to configuration.toMap(),
      "device" to device?.toMap(),
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
    )
}

data class DeviceMetadata(val deviceSpec: String?, val widthDp: Int?, val heightDp: Int?) {
  fun toMap(): Map<String, Any?> =
    mapOf("deviceSpec" to deviceSpec, "widthDp" to widthDp, "heightDp" to heightDp)
}
