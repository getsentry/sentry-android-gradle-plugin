package io.sentry.android.gradle.util

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assume.assumeThat
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class SkipOnForksRule : TestRule {
  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        assumeThat(
          "Sentry Source Context only supported when SENTRY_AUTH_TOKEN is present",
          System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty(),
          `is`(false),
        )
        base.evaluate()
      }
    }
  }
}
