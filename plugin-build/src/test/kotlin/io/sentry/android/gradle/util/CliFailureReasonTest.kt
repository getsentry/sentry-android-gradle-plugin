package io.sentry.android.gradle.util

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CliFailureReasonTest(
    private val stdErr: String,
    private val expectedReason: CliFailureReason
) {

    @Test
    fun `parses reason from stderr`() {
        val message = "somethingsomething\n$stdErr"

        val reason = CliFailureReason.fromErrOut(message)

        assertEquals(expectedReason, reason)
    }

    companion object {
        @Parameterized.Parameters(name = "{1}")
        @JvmStatic
        fun parameters() = listOf(
            arrayOf("error: resource not found", CliFailureReason.OUTDATED),
            arrayOf("error: An organization slug is required", CliFailureReason.ORG_SLUG),
            arrayOf("error: A project slug is required", CliFailureReason.PROJECT_SLUG),
            arrayOf(
                "error: Failed to parse org auth token",
                CliFailureReason.INVALID_ORG_AUTH_TOKEN
            ),
            arrayOf("error: we don't do that here", CliFailureReason.UNKNOWN)
        )
    }
}
