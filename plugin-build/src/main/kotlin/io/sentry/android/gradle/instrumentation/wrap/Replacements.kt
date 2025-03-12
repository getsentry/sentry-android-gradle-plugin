package io.sentry.android.gradle.instrumentation.wrap

data class Replacement(
    val owner: String,
    val name: String,
    val descriptor: String
) {
    object FileInputStream {
        val STRING = Replacement(
            "java/io/FileInputStream",
            "<init>",
            "(Ljava/lang/String;)V"
        ) to Replacement(
            "io/sentry/instrumentation/file/SentryFileInputStream${'$'}Factory",
            "create",
            "(Ljava/io/FileInputStream;Ljava/lang/String;)Ljava/io/FileInputStream;"
        )
        val FILE = Replacement(
            "java/io/FileInputStream",
            "<init>",
            "(Ljava/io/File;)V"
        ) to Replacement(
            "io/sentry/instrumentation/file/SentryFileInputStream${'$'}Factory",
            "create",
            "(Ljava/io/FileInputStream;Ljava/io/File;)Ljava/io/FileInputStream;"
        )
        val FILE_DESCRIPTOR =
            Replacement(
                "java/io/FileInputStream",
                "<init>",
                "(Ljava/io/FileDescriptor;)V"
            ) to Replacement(
                "io/sentry/instrumentation/file/SentryFileInputStream${'$'}Factory",
                "create",
                "(Ljava/io/FileInputStream;Ljava/io/FileDescriptor;)Ljava/io/FileInputStream;"
            )
    }

    object FileOutputStream {
        val STRING =
            Replacement(
                "java/io/FileOutputStream",
                "<init>",
                "(Ljava/lang/String;)V"
            ) to Replacement(
                "io/sentry/instrumentation/file/SentryFileOutputStream${'$'}Factory",
                "create",
                "(Ljava/io/FileOutputStream;Ljava/lang/String;)Ljava/io/FileOutputStream;"
            )
        val STRING_BOOLEAN =
            Replacement(
                "java/io/FileOutputStream",
                "<init>",
                "(Ljava/lang/String;Z)V"
            ) to Replacement(
                "io/sentry/instrumentation/file/SentryFileOutputStream${'$'}Factory",
                "create",
                "(Ljava/io/FileOutputStream;Ljava/lang/String;Z)Ljava/io/FileOutputStream;"
            )
        val FILE = Replacement(
            "java/io/FileOutputStream",
            "<init>",
            "(Ljava/io/File;)V"
        ) to Replacement(
            "io/sentry/instrumentation/file/SentryFileOutputStream${'$'}Factory",
            "create",
            "(Ljava/io/FileOutputStream;Ljava/io/File;)Ljava/io/FileOutputStream;"
        )
        val FILE_BOOLEAN =
            Replacement(
                "java/io/FileOutputStream",
                "<init>",
                "(Ljava/io/File;Z)V"
            ) to Replacement(
                "io/sentry/instrumentation/file/SentryFileOutputStream${'$'}Factory",
                "create",
                "(Ljava/io/FileOutputStream;Ljava/io/File;Z)Ljava/io/FileOutputStream;"
            )
        val FILE_DESCRIPTOR =
            Replacement(
                "java/io/FileOutputStream",
                "<init>",
                "(Ljava/io/FileDescriptor;)V"
            ) to Replacement(
                "io/sentry/instrumentation/file/SentryFileOutputStream${'$'}Factory",
                "create",
                "(Ljava/io/FileOutputStream;Ljava/io/FileDescriptor;)Ljava/io/FileOutputStream;"
            )
    }

    object Context {
        val OPEN_FILE_INPUT =
            Replacement(
                "",
                "openFileInput",
                "(Ljava/lang/String;)Ljava/io/FileInputStream;"
            ) to Replacement(
                "io/sentry/instrumentation/file/SentryFileInputStream${'$'}Factory",
                "create",
                "(Ljava/io/FileInputStream;Ljava/lang/String;)Ljava/io/FileInputStream;"
            )

        val OPEN_FILE_OUTPUT =
            Replacement(
                "",
                "openFileOutput",
                "(Ljava/lang/String;I)Ljava/io/FileOutputStream;"
            ) to Replacement(
                "io/sentry/instrumentation/file/SentryFileOutputStream${'$'}Factory",
                "create",
                "(Ljava/io/FileOutputStream;Ljava/lang/String;)Ljava/io/FileOutputStream;"
            )
    }
}
