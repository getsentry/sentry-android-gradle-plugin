package io.sentry.android.gradle.transforms.fakes

import java.io.File
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.junit.rules.TemporaryFolder

class FakeTransformOutputs(temporaryFolder: TemporaryFolder) : TransformOutputs {
    val rootDir: File = temporaryFolder.newFolder()

    lateinit var outputDirectory: File
        private set
    lateinit var outputFile: File
        private set

    override fun file(path: Any): File {
        outputFile = if (path is Provider<*>) {
            (path.get() as FileSystemLocation).asFile
        } else if (path is File && path.isAbsolute) {
            path
        } else {
            path as String
            rootDir.resolve(path)
        }
        return outputFile
    }

    override fun dir(path: Any): File {
        outputDirectory = if (path is File && path.isAbsolute) {
            path
        } else {
            path as String
            rootDir.resolve(path).also { it.mkdirs() }
        }
        return outputDirectory
    }
}
