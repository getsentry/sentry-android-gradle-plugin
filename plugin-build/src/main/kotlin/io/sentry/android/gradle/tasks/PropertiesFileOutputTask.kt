package io.sentry.android.gradle.tasks

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "abstract task, should not be used directly")
abstract class PropertiesFileOutputTask : DirectoryOutputTask() {
  @get:Internal abstract val outputFile: Provider<RegularFile>
}
