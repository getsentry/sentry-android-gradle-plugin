package io.sentry.android.gradle.util

import org.gradle.api.provider.ProviderFactory

/**
 * Inspired by:
 * https://github.com/gradle/common-custom-user-data-gradle-plugin/blob/280c9d37596a70c531111cd15824f375edd3c515/src/main/java/com/gradle/CiUtils.java
 */
internal object CiUtils {
  fun ProviderFactory.isCi(): Boolean {
    return isGenericCI() ||
      isJenkins() ||
      isHudson() ||
      isTeamCity() ||
      isCircleCI() ||
      isBamboo() ||
      isGitHubActions() ||
      isGitLab() ||
      isTravis() ||
      isBitrise() ||
      isGoCD() ||
      isAzurePipelines() ||
      isBuildkite()
  }

  fun ProviderFactory.isGenericCI(): Boolean {
    return environmentVariable("CI").isPresent || systemProperty("CI").isPresent
  }

  fun ProviderFactory.isJenkins(): Boolean {
    return environmentVariable("JENKINS_URL").isPresent
  }

  fun ProviderFactory.isHudson(): Boolean {
    return environmentVariable("HUDSON_URL").isPresent
  }

  fun ProviderFactory.isTeamCity(): Boolean {
    return environmentVariable("TEAMCITY_VERSION").isPresent
  }

  fun ProviderFactory.isCircleCI(): Boolean {
    return environmentVariable("CIRCLE_BUILD_URL").isPresent
  }

  fun ProviderFactory.isBamboo(): Boolean {
    return environmentVariable("bamboo_resultsUrl").isPresent
  }

  fun ProviderFactory.isGitHubActions(): Boolean {
    return environmentVariable("GITHUB_ACTIONS").isPresent
  }

  fun ProviderFactory.isGitLab(): Boolean {
    return environmentVariable("GITLAB_CI").isPresent
  }

  fun ProviderFactory.isTravis(): Boolean {
    return environmentVariable("TRAVIS_JOB_ID").isPresent
  }

  fun ProviderFactory.isBitrise(): Boolean {
    return environmentVariable("BITRISE_BUILD_URL").isPresent
  }

  fun ProviderFactory.isGoCD(): Boolean {
    return environmentVariable("GO_SERVER_URL").isPresent
  }

  fun ProviderFactory.isAzurePipelines(): Boolean {
    return environmentVariable("TF_BUILD").isPresent
  }

  fun ProviderFactory.isBuildkite(): Boolean {
    return environmentVariable("BUILDKITE").isPresent
  }
}
