package io.sentry.android.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.reandroid.lib.apk.ApkModule
import io.sentry.android.gradle.sourcecontext.GenerateBundleIdTask.Companion.SENTRY_BUNDLE_ID_PROPERTY
import io.sentry.android.gradle.testutil.forceEvaluate
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.android.gradle.util.SemVer
import io.sentry.gradle.common.AndroidVariant
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.io.inputstream.ZipInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

/* ktlint-disable max-line-length */
private val ASSET_PATTERN_PROGUARD =
    Regex(
        """^io\.sentry\.ProguardUuids=([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})$"""
            .trimMargin()
    )

private val ASSET_PATTERN_SOURCE_CONTEXT =
    Regex(
        """^$SENTRY_BUNDLE_ID_PROPERTY=([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})$"""
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
    val matcher = sentryProperties.lines().mapNotNull { line ->
        ASSET_PATTERN_PROGUARD.matchEntire(line)
    }.firstOrNull()

    assertTrue("Properties file is missing from the APK") { sentryProperties.isNotBlank() }
    assertNotNull(matcher, "$sentryProperties does not match pattern $ASSET_PATTERN_PROGUARD")

    return UUID.fromString(matcher.groupValues[1])
}

internal fun verifySourceContextId(
    rootFile: File,
    variant: String = "release",
    signed: Boolean = true
): UUID {
    val signedStr = if (signed) "-unsigned" else ""
    val apk = rootFile.resolve("app/build/outputs/apk/$variant/app-$variant$signedStr.apk")
    val sentryProperties = extractZip(apk, "assets/sentry-debug-meta.properties")
    val matcher = sentryProperties.lines().mapNotNull { line ->
        ASSET_PATTERN_SOURCE_CONTEXT.matchEntire(line)
    }.firstOrNull()

    assertTrue("Properties file is missing from the APK") { sentryProperties.isNotBlank() }
    assertNotNull(matcher, "$sentryProperties does not match pattern $ASSET_PATTERN_SOURCE_CONTEXT")

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
    val zip = ZipFile(zipFile)
    try {
        zip.getInputStream(zip.getFileHeader(fileToExtract)).use { zis ->
            return readZippedContent(zis)
        }
    } catch (e: ZipException) {
        println("No entry $fileToExtract in $zipFile")
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

fun TemporaryFolder.withDummyKtFile(): String {
    val contents =
        // language=kotlin
        """
            package com.example

            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.text.BasicText

            @Composable
            fun FancyButton() {
                BasicText("Hello World")
            }
        """.trimIndent()
    val sourceFile =
        File(newFolder("app/src/main/kotlin/com/example/"), "Example.kt")

    sourceFile.writeText(contents)
    return contents
}

fun TemporaryFolder.withDummyJavaFile(): String {
    val contents =
        // language=java
        """
            package com.example;

            public class TestJava {
            }
        """.trimIndent()
    val sourceFile =
        File(newFolder("app/src/main/java/com/example/"), "TestJava.java")

    sourceFile.writeText(contents)
    return contents
}

fun TemporaryFolder.withDummyCustomFile(): String {
    val contents =
        // language=kotlin
        """
            package io.other

            class TestKotlin {
              fun math(a: Int) = a * 2
            }
        """.trimIndent()
    val sourceFile =
        File(newFolder("app/src/custom/kotlin/io/other/"), "TestCustom.kt")

    sourceFile.writeText(contents)
    return contents
}

internal fun verifySourceBundleContents(
    rootFile: File,
    sourceFilePath: String,
    contents: String,
    variant: String = "release"
) {
    // first, extract the bundle-id to find the source bundle later in "/intermediates/sentry/"
    val apk = rootFile.resolve("app/build/outputs/apk/$variant/app-$variant-unsigned.apk")
    val sentryProperties = extractZip(apk, "assets/sentry-debug-meta.properties")
    val matcher = sentryProperties.lines().mapNotNull { line ->
        ASSET_PATTERN_SOURCE_CONTEXT.matchEntire(line)
    }.firstOrNull()
    assertTrue("Properties file is missing from the APK") { sentryProperties.isNotBlank() }
    assertNotNull(matcher, "$sentryProperties does not match pattern $ASSET_PATTERN_SOURCE_CONTEXT")
    val sourceBundleId = matcher.groupValues[1]

    // then, extract the source bundle zip file contents and verify them against expected contents
    val sourceBundle = rootFile.resolve("app/build/intermediates/sentry/$variant/source-bundle/$sourceBundleId.zip")
    val sourceFileContents = extractZip(sourceBundle, sourceFilePath)

    assertEquals(contents, sourceFileContents, "$sourceFilePath contents do not match $contents")
}
