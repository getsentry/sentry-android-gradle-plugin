package io.sentry.android.gradle.transforms

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.sentry.android.gradle.transforms.fakes.FakeTransformOutputs
import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.provider.DefaultProperty
import org.gradle.api.internal.provider.PropertyHost
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MetaInfStripTransformTest {

    class Fixture {
        val provider = mock<Provider<FileSystemLocation>>()

        fun getSut(
            tmpDir: TemporaryFolder,
            jarName: String = "test.jar",
            multiRelease: Boolean = true,
            includeSupportedVersion: Boolean = false,
            signed: Boolean = false
        ): MetaInfStripTransform {
            val file = tmpDir.newFile(jarName)
            val jar = file.toJar(
                multiRelease = multiRelease,
                includeSupportedVersion = includeSupportedVersion,
                signed = signed
            )

            whenever(provider.get()).thenReturn(RegularFile { jar })

            return TestMetaInfStripTransform(provider)
        }

        private fun File.toJar(
            multiRelease: Boolean,
            includeSupportedVersion: Boolean,
            signed: Boolean
        ): File {
            JarOutputStream(outputStream()).use {
                // normal classpath
                it.putNextEntry(ZipEntry("com/squareup/moshi/Types.class"))
                it.write(
                    """
                    public final class Types {
                    }
                    """.trimIndent().toByteArray()
                )

                if (multiRelease) {
                    // META-INF/versions/16
                    it.putNextEntry(
                        ZipEntry("META-INF/versions/16/com/squareup/moshi/RecordJsonAdapter.class")
                    )
                    it.write(
                        """
                        import com.squareup.moshi.RecordJsonAdapter.1;

                        final class RecordJsonAdapter extends JsonAdapter {
                            //...
                        }
                        """.trimIndent().toByteArray()
                    )
                    it.closeEntry()

                    it.putNextEntry(
                        ZipEntry(
                            "META-INF/versions/16/com/squareup/moshi/RecordJsonAdapter$1.class"
                        )
                    )
                    it.write(
                        """
                        import com.squareup.moshi.JsonAdapter.Factory;

                        final class RecordJsonAdapter$1 implements JsonAdapter.Factory {
                            //...
                        }
                        """.trimIndent().toByteArray()
                    )
                    it.closeEntry()

                    // META-INF/versions/9
                    if (includeSupportedVersion) {
                        it.putNextEntry(ZipEntry("META-INF/versions/9/module-info.class"))
                        it.write(
                            """
                            class module-info {
                                 <ClassVersion=53>
                                 <SourceFile=module-info.java>
                            }
                            """.trimIndent().toByteArray()
                        )
                        it.closeEntry()
                    }

                    if (signed) {
                        it.putNextEntry(ZipEntry("META-INF/SIGNING_FILE.SF"))
                        it.putNextEntry(ZipEntry("META-INF/SIGNING_FILE.DSA"))
                    }
                }

                // manifest
                it.putNextEntry(ZipEntry(JarFile.MANIFEST_NAME))
                val manifest = Manifest().apply {
                    mainAttributes[Attributes.Name.MULTI_RELEASE] = multiRelease.toString()
                    mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
                }
                manifest.write(it)
                it.closeEntry()
            }
            return this
        }

        private class TestMetaInfStripTransform(
            override val inputArtifact: Provider<FileSystemLocation>
        ) : MetaInfStripTransform() {
            override fun getParameters(): Parameters = object : Parameters {
                override val invalidate: Property<Long>
                    get() = DefaultProperty(PropertyHost.NO_OP, Long::class.java).convention(0L)
            }
        }
    }

    @get:Rule
    val tmp = TemporaryFolder()

    private val fixture = Fixture()

    @Test
    fun `when not multi-release jar, keeps input unchanged`() {
        val outputs = FakeTransformOutputs(tmp)

        val sut = fixture.getSut(tmp, multiRelease = false)
        sut.transform(outputs)

        assertTrue { outputs.outputFile.name == "test.jar" }
    }

    @Test
    fun `when multi-release jar, renames input with meta-inf-stripped suffix`() {
        val outputs = FakeTransformOutputs(tmp)

        val sut = fixture.getSut(tmp)
        sut.transform(outputs)

        assertTrue { outputs.outputFile.name == "test-meta-inf-stripped.jar" }
    }

    @Test
    fun `when multi-release jar, strips out unsupported java classes`() {
        val outputs = FakeTransformOutputs(tmp)

        val sut = fixture.getSut(tmp)

        sut.transform(outputs)

        val jar = JarFile(outputs.outputFile)
        assertFalse { jar.read().any { it.key.startsWith("META-INF/versions/16") } }
    }

    @Test
    fun `when multi-release jar contains only unsupported classes, changes to single-release`() {
        val outputs = FakeTransformOutputs(tmp)

        val sut = fixture.getSut(tmp)
        sut.transform(outputs)

        val jar = JarFile(outputs.outputFile)
        assertFalse { jar.isMultiRelease }
    }

    @Test
    fun `when multi-release jar with supported classes, keeps them and multi-release flag`() {
        val outputs = FakeTransformOutputs(tmp)

        val sut = fixture.getSut(tmp, includeSupportedVersion = true)
        sut.transform(outputs)

        val jar = JarFile(outputs.outputFile)
        assertEquals(
            jar.read()["META-INF/versions/9/module-info.class"],
            """
            class module-info {
                 <ClassVersion=53>
                 <SourceFile=module-info.java>
            }
            """.trimIndent()
        )
        assertTrue { jar.isMultiRelease }
    }

    @Test
    fun `when multi-release jar, keeps unrelated entries unchanged`() {
        val outputs = FakeTransformOutputs(tmp)

        val sut = fixture.getSut(tmp)
        sut.transform(outputs)

        val jar = JarFile(outputs.outputFile)
        assertEquals(
            jar.read()["com/squareup/moshi/Types.class"],
            """
            public final class Types {
            }
            """.trimIndent()
        )
        assertTrue { jar.manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] == "1.0" }
    }

    private fun JarFile.read(): Map<String, String> {
        val entries = mutableMapOf<String, String>() // entry.name to entry.content
        val iter = entries()
        while (iter.hasMoreElements()) {
            val jarEntry = iter.nextElement()
            entries[jarEntry.name] = getInputStream(jarEntry).reader().readText()
        }
        return entries
    }

    @Test
    fun `when multi-release signed-jar, skip transform`() {
        val outputs = FakeTransformOutputs(tmp)

        val sut = fixture.getSut(tmp, includeSupportedVersion = true, signed = true)
        sut.transform(outputs)

        val jar = JarFile(outputs.outputFile)

        assertTrue { jar.read().any { it.key.startsWith("META-INF/versions/16") } }
        assertTrue { outputs.outputFile.name == "test.jar" }
    }
}
