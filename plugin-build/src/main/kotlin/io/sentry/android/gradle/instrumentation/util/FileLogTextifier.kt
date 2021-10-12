package io.sentry.android.gradle.instrumentation.util

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import org.objectweb.asm.util.Textifier

class FileLogTextifier(
    apiVersion: Int,
    log: File,
    methodName: String?,
    methodDescriptor: String?
) : Textifier(apiVersion) {

    private val fileOutputStream = FileOutputStream(log, true).apply {
        write("function $methodName $methodDescriptor".toByteArray())
        write(System.lineSeparator().toByteArray())
    }

    override fun visitMethodEnd() {
        val printWriter = PrintWriter(fileOutputStream)
        print(printWriter)
        printWriter.flush()
        fileOutputStream.write(System.lineSeparator().toByteArray())
        fileOutputStream.close()
    }
}
