package io.sentry.android.gradle.instrumentation.util

import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.Textifier
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

class FileLogTextifier(
    log: File,
    methodName: String?,
    methodDescriptor: String?
) : Textifier(Opcodes.ASM7) {

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
