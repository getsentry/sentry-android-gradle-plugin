package io.sentry.android.gradle

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ManifestMetadataParserTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test
  fun `parses sentry metadata with PackageManager value types`() {
    val manifest =
      manifest(
        """
        <meta-data android:name="io.sentry.debug" android:value="true"/>
        <meta-data android:name="io.sentry.enabled" android:value="false"/>
        <meta-data android:name="io.sentry.max-breadcrumbs" android:value="42"/>
        <meta-data android:name="io.sentry.hex" android:value="-0x2A"/>
        <meta-data android:name="io.sentry.sample-rate" android:value="0.5"/>
        <meta-data android:name="io.sentry.dsn" android:value="https://example.invalid/1"/>
        <meta-data android:name="other.metadata" android:value="ignored"/>
        """
      )

    assertThat(ManifestMetadataParser.parse(manifest))
      .containsExactly(
        "io.sentry.debug",
        true,
        "io.sentry.enabled",
        false,
        "io.sentry.max-breadcrumbs",
        42,
        "io.sentry.hex",
        -42,
        "io.sentry.sample-rate",
        0.5f,
        "io.sentry.dsn",
        "https://example.invalid/1",
      )
  }

  @Test
  fun `returns null for resource references`() {
    assertThat(
        ManifestMetadataParser.parse(
          manifest("""<meta-data android:name="io.sentry.dsn" android:value="@string/dsn"/>""")
        )
      )
      .isNull()
    assertThat(
        ManifestMetadataParser.parse(
          manifest("""<meta-data android:name="io.sentry.dsn" android:resource="@string/dsn"/>""")
        )
      )
      .isNull()
  }

  @Test
  fun `returns null for unresolved placeholders`() {
    assertThat(
        ManifestMetadataParser.parse(
          manifest("""<meta-data android:name="io.sentry.dsn" android:value="${'$'}{dsn}"/>""")
        )
      )
      .isNull()
  }

  private fun manifest(metadata: String): File =
    temporaryFolder.newFile("AndroidManifest-${temporaryFolder.root.listFiles()?.size}.xml").apply {
      writeText(
        """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
          <application>
            $metadata
          </application>
        </manifest>
        """
          .trimIndent()
      )
    }
}
