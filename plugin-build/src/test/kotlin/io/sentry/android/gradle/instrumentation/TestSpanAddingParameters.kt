package io.sentry.android.gradle.instrumentation

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.DefaultFileCollectionFactory
import org.gradle.api.internal.file.DefaultFileLookup
import org.gradle.api.internal.file.DefaultFilePropertyFactory
import org.gradle.api.internal.file.FileCollectionFactory
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory
import org.gradle.api.internal.provider.DefaultProperty
import org.gradle.api.internal.provider.PropertyHost
import org.gradle.api.internal.tasks.DefaultTaskDependencyFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.util.internal.PatternSets
import org.gradle.internal.nativeintegration.services.FileSystems
import org.gradle.internal.nativeintegration.services.NativeServices
import java.io.File

class TestSpanAddingParameters(
    private val inMemoryDir: File
) : SpanAddingClassVisitorFactory.SpanAddingParameters {

    init {
        NativeServices.initialize(inMemoryDir)
    }

    override val invalidate: Property<Long>
        get() = DefaultProperty(PropertyHost.NO_OP, Long::class.java).convention(0L)

    override val debug: Property<Boolean>
        get() = DefaultProperty(PropertyHost.NO_OP, Boolean::class.javaObjectType).convention(true)

    override val tmpDir: RegularFileProperty
        get() = filePropertyFactory.newFileProperty().value(filePropertyFactory.file(inMemoryDir))

    private val fileLookup: DefaultFileLookup = DefaultFileLookup()

    private val fileCollectionFactory: FileCollectionFactory =
        DefaultFileCollectionFactory(
            fileLookup.pathToFileResolver,
            DefaultTaskDependencyFactory.withNoAssociatedProject(),
            DefaultDirectoryFileTreeFactory(),
            PatternSets.getNonCachingPatternSetFactory(),
            PropertyHost.NO_OP,
            FileSystems.getDefault()
        )

    private val filePropertyFactory: DefaultFilePropertyFactory =
        DefaultFilePropertyFactory(
            PropertyHost.NO_OP,
            fileLookup.fileResolver,
            fileCollectionFactory
        )
}
