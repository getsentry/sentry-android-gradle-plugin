package io.sentry.android.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

abstract class GenerateSentryProguardUuidTask extends DefaultTask {
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Internal
    public Provider<UUID> getOutputUuid() {
        return getOutputUuidInternal();
    }

    @Internal
    protected abstract Property<UUID> getOutputUuidInternal();

    @Internal
    private final Provider<RegularFile> outputFile = getOutputDirectory().map(new Transformer<RegularFile, Directory>() {
        @NotNull
        @Override
        public RegularFile transform(@NotNull Directory dir) {
            return dir.file("sentry-debug-meta.properties");
        }
    });

    public GenerateSentryProguardUuidTask() {
        getOutputs().upToDateWhen(new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task spec) {
                return false;
            }
        });
        setDescription("Generates a unique build ID");
    }

    @Internal
    public Provider<RegularFile> getOutputFile() {
        return outputFile;
    }

    @TaskAction
    protected void generateProperties() throws IOException {
        UUID uuid = UUID.randomUUID();
        getOutputUuidInternal().set(uuid);

        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile.get().getAsFile())));
        try {
            writer.write("io.sentry.ProguardUuids=");
            writer.write(uuid.toString());
        } finally {
            writer.close();
        }
    }
}
