package io.sentry.gradle.common

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

data class JavaVariant(val project: Project, val javaExtension: JavaPluginExtension) :
  SentryVariant {
  override val name: String = "java"
  override val flavorName = null
  override val buildTypeName = null
  override val productFlavors = emptyList<String>()
  override val isMinifyEnabled = false
  override val isDebuggable = false

  override val assembleProvider: TaskProvider<out Task>?
    get() = project.tasks.named("assemble", DefaultTask::class.java)

  override fun mappingFileProvider(project: Project): Provider<FileCollection> {
    return project.provider { project.files() }
  }

  override fun sources(
    project: Project,
    additionalSources: Provider<out Collection<Directory>>,
  ): Provider<out Collection<Directory>> {
    val projectDir = project.layout.projectDirectory
    return project.provider {
      val javaDirs =
        javaExtension.sourceSets.flatMap {
          it.allJava.sourceDirectories.map { javaDir -> projectDir.dir(javaDir.absolutePath) }
        }
      (javaDirs + additionalSources.get()).filterBuildConfig().toSet()
    }
  }
}
