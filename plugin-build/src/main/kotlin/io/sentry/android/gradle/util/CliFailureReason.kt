package io.sentry.android.gradle.util

enum class CliFailureReason(val message: (taskName: String) -> String) {
    OUTDATED({
        """
    The task '$it' hit a non-existing endpoint on Sentry.
    Most likely you have to update your self-hosted Sentry version to get all of the latest features.
        """.trimIndent()
    }),
    ORG_SLUG({
        """
    An organization slug is required. You might want to provide your Sentry org name via the 'sentry.properties' file:
    ```
    defaults.org=my-org
    ```

    or via configuring the 'sentry' gradle plugin extension:
    ```
    sentry {
      org.set("my-org")
    }
        """.trimIndent()
    }),
    PROJECT_SLUG({
        """
    A project slug is required. You might want to provide your Sentry project name via the 'sentry.properties' file:
    ```
    defaults.project=my-project
    ```

    or via configuring the 'sentry' gradle plugin extension:
    ```
    sentry {
      projectName.set("my-project")
    }
    ```
        """.trimIndent()
    }),
    INVALID_ORG_AUTH_TOKEN({
        """
    Failed to parse org auth token. You might want to provide a custom url to your self-hosted Sentry instance via the 'sentry.properties' file:
    ```
    defaults.url=https://mysentry.invalid/
    ```

    or via configuring the 'sentry' gradle plugin extension:
    ```
    sentry {
      url.set("https://mysentry.invalid/")
    }
    ```
        """.trimIndent()
    }),
    UNKNOWN({
        """
    An error occurred while executing task '$it'. Please check the detailed sentry-cli output above.
        """.trimIndent()
    });

    companion object {
        fun fromErrOut(errOut: String): CliFailureReason {
            return when {
                errOut.contains("error: resource not found") -> OUTDATED
                errOut.contains("error: An organization slug is required") -> ORG_SLUG
                errOut.contains("error: A project slug is required") -> PROJECT_SLUG
                errOut.contains("error: Failed to parse org auth token") -> INVALID_ORG_AUTH_TOKEN
                else -> UNKNOWN
            }
        }
    }
}
