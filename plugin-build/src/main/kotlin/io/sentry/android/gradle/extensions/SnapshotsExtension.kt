package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
open class SnapshotsExtension @Inject constructor(objects: ObjectFactory) {

  /** The path to the folder containing snapshots to upload. */
  val path: DirectoryProperty = objects.directoryProperty()
}
