package io.sentry.android.gradle.internal

import java.io.FileOutputStream
import java.util.zip.ZipFile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel

/**
 * A port of https://github.com/stepango/aar2jar, which adds support for 2 things:
 *  - runtimeOnlyAar configuration
 *  - Gradle 8.0+ registerTransform API
 */
class Aar2JarPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val compileOnlyAar = project.configurations.register("compileOnlyAar")
        val runtimeOnlyAar = project.configurations.register("runtimeOnlyAar")
        val implementationAar = project.configurations.register("implementationAar")

        // Assume all modules have test configuration
        val testCompileOnlyAar = project.configurations.register("testCompileOnlyAar")
        val testRuntimeOnlyAar = project.configurations.register("testRuntimeOnlyAar")
        val testImplementationAar = project.configurations.register("testImplementationAar")

        project.pluginManager.withPlugin("idea") {
            val scopes = project.extensions
                .getByType<IdeaModel>()
                .module
                .scopes

            scopes["TEST"]
                ?.get("plus")
                ?.apply {
                    add(testImplementationAar.get())
                    add(testCompileOnlyAar.get())
                    add(testRuntimeOnlyAar.get())
                }

            scopes.forEach {
                it.value["plus"]?.apply {
                    add(implementationAar.get())
                    add(compileOnlyAar.get())
                    add(runtimeOnlyAar.get())
                }
            }
        }

        project.dependencies.registerTransform(AarToJarTransform::class.java) {
            from.attribute(ARTIFACT_ATTR, "aar")
            to.attribute(ARTIFACT_ATTR, ArtifactTypeDefinition.JAR_TYPE)
        }

        compileOnlyAar.configure {
            baseConfiguration(project, "main") {
                compileClasspath += this@configure
            }
        }

        runtimeOnlyAar.configure {
            baseConfiguration(project, "main") {
                runtimeClasspath += this@configure
            }
        }

        implementationAar.configure {
            baseConfiguration(project, "main") {
                compileClasspath += this@configure
                runtimeClasspath += this@configure
            }
        }

        testCompileOnlyAar.configure {
            baseConfiguration(project, "test") {
                compileClasspath += this@configure
            }
        }

        testRuntimeOnlyAar.configure {
            baseConfiguration(project, "test") {
                runtimeClasspath += this@configure
            }
        }

        testImplementationAar.configure {
            baseConfiguration(project, "test") {
                compileClasspath += this@configure
                runtimeClasspath += this@configure
            }
        }
    }

    companion object {
        internal val ARTIFACT_ATTR = Attribute.of("artifactType", String::class.java)
    }
}

fun Configuration.baseConfiguration(project: Project, name: String, f: SourceSet.() -> Unit) {
    isTransitive = false
    attributes {
        attribute(Aar2JarPlugin.ARTIFACT_ATTR, ArtifactTypeDefinition.JAR_TYPE)
    }
    project.pluginManager.withPlugin("java") {
        val sourceSets = project.the<JavaPluginExtension>().sourceSets
        sourceSets.withName(name, f)
    }
}

fun SourceSetContainer.withName(name: String, f: SourceSet.() -> Unit) {
    this[name]?.apply { f(this) } ?: whenObjectAdded { if (this.name == name) f(this) }
}

abstract class AarToJarTransform : TransformAction<TransformParameters.None> {

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputFile = input.get().asFile
        val outputFile = outputs.file(inputFile.name.replace(".aar", ".jar"))
        ZipFile(inputFile).use { zipFile ->
            zipFile.entries()
                .toList()
                .first { it.name == "classes.jar" }
                .let(zipFile::getInputStream)
                .use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
        }
    }
}
