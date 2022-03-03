package io.sentry.android.gradle.internal

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * A task which outputs a asmified version of a given .class file. This is useful, when
 * the ASM IntelliJ Plugin fails to decompile nested synthetic classes (e.g. lambdas).
 *
 * @property clazz Path to the .class file that is going to be ASMified
 * @property output Output type, one of `console` or `file`. If set to `file`, the file is generated
 * automatically under buildDir/tmp/asmified folder. Defaults to `console`.
 *
 * Example command: cd plugin-build &&
 * ./gradlew asmify --class=../examples/android-room/build/intermediates/javac/debug/classes/io/sentry/android/roomsample/data/TracksDao_Impl\$6.class --output=file
 */
abstract class ASMifyTask : Exec() {

    enum class Output {
        CONSOLE, FILE;
    }

    @set:Option(
        option = "class",
        description = "Path to the .class file, that is going to be ASMified"
    )
    @get:Input
    var clazz: String = ""

    @set:Option(
        option = "output",
        description = "Output type, either 'console' or 'file'. If set to 'file', the file is generated automatically under buildDir/tmp/asmified folder"
    )
    @get:Input
    var output: Output = Output.CONSOLE

    @get:OptionValues("output")
    val supportedOutputs: Collection<Output> = EnumSet.allOf(Output::class.java)

    private val tmpDir: String get() = "${project.buildDir}/tmp/asmified"

    override fun exec() {
        val asmJars = project.configurations.getByName("compileClasspath")
            .resolvedConfiguration
            .resolvedArtifacts
            .filter {
                val name = it.id.componentIdentifier.displayName
                return@filter name.startsWith(ASM) || name.startsWith(ASM_UTIL)
            }
        executable = "java"
        args(
            "-classpath",
            asmJars.joinToString(separator = ":") { it.file.absolutePath },
            ASMIFIER_CLASS,
            "-nodebug",
            clazz
        )

        if (output == Output.FILE) {
            val dir = File(tmpDir)
            dir.mkdirs()

            val filename =
                clazz.substringAfterLast(File.separator).substringBefore(".") + "_asmified.java"

            val file = File(dir, filename)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            standardOutput = FileOutputStream(file)
        }

        super.exec()
    }

    companion object {
        private const val ASM_UTIL = "org.ow2.asm:asm-util:"
        private const val ASM = "org.ow2.asm:asm:"

        private const val ASMIFIER_CLASS = "org.objectweb.asm.util.ASMifier"
    }
}
