package io.sentry.android.gradle

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/* ktlint-disable max-line-length */
private val ASSET_PATTERN =
    Regex(
        """^io\.sentry\.ProguardUuids=([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})$"""
            .trimMargin()
    )
/* ktlint-enable max-line-length */

internal fun verifyProguardUuid(rootFile: File, variant: String = "release"): UUID {
    val apk = rootFile.resolve("app/build/outputs/apk/$variant/app-$variant-unsigned.apk")
    val sentryProperties = extractZip(apk, "assets/sentry-debug-meta.properties")
    val matcher = ASSET_PATTERN.matchEntire(sentryProperties)

    assertTrue("Properties file is missing from the APK") { sentryProperties.isNotBlank() }
    assertNotNull(matcher, "$sentryProperties does not match pattern $ASSET_PATTERN")

    return UUID.fromString(matcher.groupValues[1])
}

private fun extractZip(zipFile: File, fileToExtract: String): String {
    ZipInputStream(FileInputStream(zipFile)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == fileToExtract) {
                return readZippedContent(zis)
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
    return ""
}

private fun readZippedContent(zipInputStream: ZipInputStream): String {
    val baos = ByteArrayOutputStream()
    val content = ByteArray(1024)
    var len: Int = zipInputStream.read(content)
    while (len > 0) {
        baos.write(content, 0, len)
        len = zipInputStream.read(content)
    }
    val stringContent = baos.toString(Charset.defaultCharset().name())
    baos.close()
    return stringContent
}
