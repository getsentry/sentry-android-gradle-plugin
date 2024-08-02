package io.sentry.android.gradle.util

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A JUnit [TestRule] to override values of [System.getProperties] with the support of the
 * [WithSystemProperty] annotation.
 */
class SystemPropertyRule : TestRule {

  private val retain = mutableMapOf<String, String?>()

  override fun apply(statement: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        val annotation =
          description.annotations.filterIsInstance<WithSystemProperty>().firstOrNull()

        annotation?.keys?.forEachIndexed { index, key ->
          System.getProperty(key).let { oldProperty -> retain[key] = oldProperty }
          System.setProperty(key, annotation.values[index])
        }
        try {
          statement.evaluate()
        } finally {
          retain.forEach { (key, value) -> System.setProperty(key, value) }
        }
      }
    }
  }
}
