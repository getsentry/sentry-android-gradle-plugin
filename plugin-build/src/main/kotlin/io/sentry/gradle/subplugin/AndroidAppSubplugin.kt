package io.sentry.gradle.subplugin

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import io.sentry.android.gradle.autoinstall.installDependencies
import io.sentry.android.gradle.cliExecutableProvider
import io.sentry.android.gradle.configure
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.sourcecontext.SourceContext.SourceContextTasks
import io.sentry.android.gradle.util.AgpVersions
import io.sentry.gradle.artifacts.Publisher.Companion.interProjectPublisher
import io.sentry.gradle.artifacts.SgpArtifacts
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal

class AndroidAppSubplugin(
  private val project: Project,
  private val extension: SentryPluginExtension,
) {

  private val bundleIdPublisher = interProjectPublisher(
    project = project,
    artifact = SgpArtifacts.Kind.BUNDLE_ID,
  )
  private val sourceFilesPublisher = interProjectPublisher(
    project = project,
    artifact = SgpArtifacts.Kind.SOURCE_ROOTS,
  )

  fun apply(
    buildEvents: BuildEventListenerRegistryInternal,
    sentryOrgParameter: String?,
    sentryProjectParameter: String?,
    sourceContextTasks: SourceContextTasks?,
  ) =
    project.run {
      checkAgpVersion()

      val oldAGPExtension = extensions.getByType(AppExtension::class.java)
      val androidComponentsExt = extensions.getByType(AndroidComponentsExtension::class.java)
      val cliExecutable = cliExecutableProvider()

      // new API configuration
      androidComponentsExt.configure(
        this,
        extension,
        buildEvents,
        cliExecutable,
        sentryOrgParameter,
        sentryProjectParameter,
        bundleIdPublisher,
        sourceFilesPublisher,
        sourceContextTasks
      )

      // old API configuration
      oldAGPExtension.configure(
        this,
        extension,
        cliExecutable,
        sentryOrgParameter,
        sentryProjectParameter,
        buildEvents,
      )

      installDependencies(extension, true)
    }

  private fun Project.checkAgpVersion() {
    if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_0) {
      throw StopExecutionException(
        """
        Using io.sentry.android.gradle:3+ with Android Gradle Plugin < 7 is not supported.
        Either upgrade the AGP version to 7+, or use an earlier version of the Sentry
        Android Gradle Plugin. For more information check our migration guide
        https://docs.sentry.io/platforms/android/migration/#migrating-from-iosentrysentry-android-gradle-plugin-2x-to-iosentrysentry-android-gradle-plugin-300
        """
          .trimIndent()
      )
    }
  }
}
