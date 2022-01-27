package io.sentry.android.gradle.transforms

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.jar.JarFile
import kotlin.streams.toList
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Gradle's [TransformAction] that strips out unsupported Java classes from
 * resources/META-INF/versions folder of a jar. This is the case for [Multi-Release JARs](https://openjdk.java.net/jeps/238),
 * when a single jar contains classes for different Java versions.
 *
 * For Android it may have bad consequences, as the min supported Java version is 11 at the moment,
 * and this can cause incompatibilities, if AGP or other plugins erroneously consider .class files
 * under the META-INF folder during build-time transformations/instrumentation.
 *
 * The minimum supported Java version of the latest AGP is 11 -> https://developer.android.com/studio/releases/gradle-plugin#7-1-0
 */
@CacheableTransform
abstract class MetaInfStripTransform : TransformAction<MetaInfStripTransform.Parameters> {

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    interface Parameters : TransformParameters {
        // only used for development purposes
        @get:Input
        @get:Optional
        val invalidate: Property<Long>
    }

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (JarFile(input).isMultiRelease) {
            // open a jar and do changes in-place
            val uri = URI.create("jar:file:$input")
            FileSystems.newFileSystem(uri, mapOf<String, Any>()).use { fs ->
                val versionsDir = fs.getPath("META-INF/versions")
                if (Files.exists(versionsDir)) {
                    val unsupportedVersions = Files.list(versionsDir)
                        .filter { (it.fileName.toString().toIntOrNull() ?: 0) > 11 }
                        .toList()

                    // unsupportedVersions contains paths like META-INF/versions/16
                    // we walk the directory in a reverse order to delete all files/subdirectories
                    // one-by-one and eventually delete the directory itself
                    unsupportedVersions.forEach { path ->
                        Files.walk(path)
                            .sorted(Comparator.reverseOrder())
                            .forEach { Files.delete(it) }
                    }
                }
            }
        }
        outputs.file(input)
    }

    companion object {
        internal val artifactType: Attribute<String> =
            Attribute.of("artifactType", String::class.java)
        internal val metaInfStripped: Attribute<Boolean> =
            Attribute.of("meta-inf-stripped", Boolean::class.javaObjectType)

        internal fun register(dependencies: DependencyHandler, forceInstrument: Boolean) {
            // add our attribute to schema
            dependencies.attributesSchema { schema ->
                schema.attribute(metaInfStripped) { matchingStrategy ->
                    /*
                    * This is necessary so our transform is selected before the AGP's one
                    * AGP's transform chain looks roughly like that:
                    *
                    * IdentityTransform (jar to processed-jar) -> IdentityTransform
                    * (processed-jar to classes-jar) AsmClassesTransform (classes-jar to asm-transformed-classes-jar)
                    *
                    * We want out transform to run before the first IdentityTransform, so the chain
                    * would look like that:
                    *
                    * MetaInfStripTransform (jar to processed-jar) -> IdentityTransform (jar to processed-jar)
                    * -> ... -> ...
                    *
                    * Since the first two transforms have conflicting attributes, we define a
                    * disambiguation rule to make Gradle select our transform first.
                    */
                    matchingStrategy.disambiguationRules
                        .pickFirst { o1, o2 -> if (o1 > o2) -1 else if (o1 < o2) 1 else 0 }
                }
            }
            // sets meta-inf-stripped attr to false for all .jar artifacts
            dependencies.artifactTypes.named("jar") {
                it.attributes.attribute(metaInfStripped, false)
            }
            // registers a transform
            dependencies.registerTransform(MetaInfStripTransform::class.java) {
                it.from.attribute(metaInfStripped, false)
                    .attribute(artifactType, "jar")

                it.to.attribute(metaInfStripped, true)
                    .attribute(artifactType, "processed-jar")

                if (forceInstrument) {
                    it.parameters.invalidate.set(System.currentTimeMillis())
                }
            }
        }
    }
}
