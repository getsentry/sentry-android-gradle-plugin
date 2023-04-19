package io.sentry.android.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.OutputDirectory

abstract class PropertiesFileOutputTask : DirectoryOutputTask() {
    abstract val outputFile: Provider<RegularFile>;
}
