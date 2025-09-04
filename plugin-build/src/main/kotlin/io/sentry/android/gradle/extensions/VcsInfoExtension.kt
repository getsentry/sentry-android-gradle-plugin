package io.sentry.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * Configuration for version control system information used in build uploads.
 * 
 * This extension only applies to build upload functionality (e.g., APK/AAB uploads for size analysis)
 * and has no effect on other plugin features such as ProGuard mapping uploads, source context uploads,
 * or instrumentation.
 */
open class VcsInfoExtension @Inject constructor(objects: ObjectFactory) {

  /**
   * The VCS commit sha to use for the upload. If not provided, the current commit sha will be used.
   */
  val headSha: Property<String> = objects.property(String::class.java).convention(null as String?)

  /**
   * The VCS commit's base sha to use for the upload. If not provided, the merge-base of the current
   * and remote branch will be used.
   */
  val baseSha: Property<String> = objects.property(String::class.java).convention(null as String?)

  /** The VCS provider to use for the upload. If not provided, the current provider will be used. */
  val vcsProvider: Property<String> =
    objects.property(String::class.java).convention(null as String?)

  /**
   * The name of the git repository to use for the upload (e.g. organization/repository). If not
   * provided, the current repository will be used.
   */
  val headRepoName: Property<String> =
    objects.property(String::class.java).convention(null as String?)

  /**
   * The name of the git repository to use for the upload (e.g. organization/repository). If not
   * provided, the current repository will be used.
   */
  val baseRepoName: Property<String> =
    objects.property(String::class.java).convention(null as String?)

  /**
   * The reference (branch) to use for the upload. If not provided, the current reference will be
   * used.
   */
  val headRef: Property<String> = objects.property(String::class.java).convention(null as String?)

  /**
   * The base reference (branch) to use for the upload. If not provided, the merge-base with the
   * remote tracking branch will be used.
   */
  val baseRef: Property<String> = objects.property(String::class.java).convention(null as String?)

  /**
   * The pull request number to use for the upload. If not provided, the current pull request number
   * will be used.
   */
  val prNumber: Property<Int> = objects.property(Int::class.java).convention(null as Int?)
}
