package io.sentry.android.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory

abstract class DirectoryOutputTask : DefaultTask() {

  @get:OutputDirectory abstract val output: DirectoryProperty
}
