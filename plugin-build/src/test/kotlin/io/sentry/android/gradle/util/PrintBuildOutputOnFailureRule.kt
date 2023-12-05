package io.sentry.android.gradle.util

import java.io.ByteArrayOutputStream
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A rule that prints integration test build logs on any failure from the provided [out] stream.
 */
class PrintBuildOutputOnFailureRule(private val out: ByteArrayOutputStream) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (e: Throwable) {
                    print(out.toString())
                    throw e
                }
            }
        }
    }
}
