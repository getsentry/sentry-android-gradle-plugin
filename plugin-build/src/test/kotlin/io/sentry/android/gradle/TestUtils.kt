package io.sentry.android.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.reandroid.lib.apk.ApkModule
import io.sentry.android.gradle.tasks.SentryWriteProguardUUIDToManifestTask
import io.sentry.android.gradle.testutil.forceEvaluate
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.SemVer
import io.sentry.gradle.common.AndroidVariant
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project

/* ktlint-disable max-line-length */
private val ASSET_PATTERN =
    Regex(
        """^io\.sentry\.ProguardUuids=([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})$"""
            .trimMargin()
    )
/* ktlint-enable max-line-length */

internal fun verifyProguardUuid(
    rootFile: File,
    variant: String = "release",
    signed: Boolean = true
): UUID {
    val signedStr = if (signed) "-unsigned" else ""
    val apk = rootFile.resolve("app/build/outputs/apk/$variant/app-$variant$signedStr.apk")
    val sentryProperties = extractZip(apk, "assets/sentry-debug-meta.properties")
    val matcher = ASSET_PATTERN.matchEntire(sentryProperties)

    assertTrue("Properties file is missing from the APK") { sentryProperties.isNotBlank() }
    assertNotNull(matcher, "$sentryProperties does not match pattern $ASSET_PATTERN")

    return UUID.fromString(matcher.groupValues[1])
}

internal fun verifyIntegrationList(
    rootFile: File,
    variant: String = "release",
    signed: Boolean = true
): List<String> {
    return retrieveMetaDataFromManifest(
        rootFile,
        variant,
        "io.sentry.gradle-plugin-integrations",
        signed
    )
        .split(',')
}

internal fun verifyProguardUuidInManifest(
    rootFile: File,
    variant: String = "release",
    signed: Boolean = true
): String {
    return retrieveMetaDataFromManifest(
        rootFile,
        variant,
        SentryWriteProguardUUIDToManifestTask.ATTR_PROGUARD_UUID,
        signed
    )
}

internal fun retrieveMetaDataFromManifest(
    rootFile: File,
    variant: String = "release",
    metaDataName: String,
    signed: Boolean = true
): String {
    val signedStr = if (signed) "-unsigned" else ""
    val apk = rootFile.resolve("app/build/outputs/apk/$variant/app-$variant$signedStr.apk")

    val apkModule = ApkModule.loadApkFile(apk)
    val attribute = apkModule.androidManifestBlock

    val metaDataValue = attribute.applicationElement.listElements()
        .filter { it.tag == "meta-data" }
        .filter { it.searchAttributeByName("name").valueAsString == metaDataName }
        .map { it.searchAttributeByName("value").valueAsString }.first()

    return metaDataValue
}

internal fun verifyDependenciesReportAndroid(
    rootFile: File,
    variant: String = "debug"
): String {
    val apk = rootFile.resolve("app/build/outputs/apk/$variant/app-$variant.apk")
    val dependenciesReport = extractZip(apk, "assets/sentry-external-modules.txt")

    assertTrue("Dependencies file is missing from the APK") { dependenciesReport.isNotBlank() }
    return dependenciesReport
}

internal fun verifyDependenciesReportJava(
    rootFile: File
): String {
    val apk = rootFile.resolve("module/build/libs/module.jar")
    val dependenciesReport = extractZip(apk, "sentry-external-modules.txt")

    assertTrue("Dependencies file is missing from the APK") { dependenciesReport.isNotBlank() }
    return dependenciesReport
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

fun Project.retrieveAndroidVariant(agpVersion: SemVer, variantName: String): AndroidVariant {
    return if (AgpVersions.isAGP74(agpVersion)) {
        var debug: Variant? = null
        val extension = project.extensions.getByType(AndroidComponentsExtension::class.java)
        extension.onVariants(extension.selector().withName(variantName)) {
            debug = it
        }
        project.forceEvaluate()

        return AndroidVariant74(debug!!)
    } else {
        val variant = project
            .extensions
            .getByType(AppExtension::class.java)
            .applicationVariants.first { it.name == variantName }
        AndroidVariant70(variant)
    }
}
