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
) : Textifier(apiVersion), ExceptionHandler {

    private var hasThrown = false

    private val fileOutputStream = FileOutputStream(log, true).apply {
        write("function $methodName $methodDescriptor".toByteArray())
        write(System.lineSeparator().toByteArray())
    }

    override fun visitMethodEnd() {
        if (!hasThrown) {
            flushPrinter()
        }
    }

    override fun handle(exception: Exception) {
        hasThrown = true
        flushPrinter()
    }

    private fun flushPrinter() {
        val printWriter = PrintWriter(fileOutputStream)
        print(printWriter)
        printWriter.flush()
        fileOutputStream.write(System.lineSeparator().toByteArray())
        fileOutputStream.close()
    }
}
