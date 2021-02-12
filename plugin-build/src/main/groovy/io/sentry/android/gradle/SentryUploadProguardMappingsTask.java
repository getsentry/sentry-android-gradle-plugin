package io.sentry.android.gradle;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;

import java.util.ArrayList;
import java.util.UUID;

abstract class SentryUploadProguardMappingsTask extends Exec {
    @Input
    public abstract Property<String> getCliExecutable();

    @Input
    public abstract Property<UUID> getMappingsUuid();

    @InputFile
    public abstract RegularFileProperty getMappingsFile();

    @Optional
    @InputFile
    public abstract RegularFileProperty getSentryProperties();

    @Optional
    @Input
    public abstract Property<String> getSentryOrganization();

    @Optional
    @Input
    public abstract Property<String> getSentryProject();

    @Input
    public abstract Property<Boolean> getAutoUpload();

    public SentryUploadProguardMappingsTask() {
        setDescription("Uploads the proguard mappings file");
    }

    @Override
    protected void exec() {
        RegularFile sentryProperties = getSentryProperties().getOrNull();
        if (sentryProperties != null) {
            environment("SENTRY_PROPERTIES", sentryProperties);
        } else {
            getLogger().info("propsFile is null");
        }

        ArrayList<Object> args = new ArrayList<>();

        args.add(getCliExecutable().get());
        args.add("upload-proguard");
        args.add("--uuid");
        args.add(getMappingsUuid().get());
        args.add(getMappingsFile().get());

        if (!getAutoUpload().get()) {
            args.add("--no-upload");
        }

        String org = getSentryOrganization().getOrNull();
        if (org != null) {
            args.add("--org");
            args.add(org);
        }

        String project = getSentryProject().getOrNull();
        if (project != null) {
            args.add("--project");
            args.add(project);
        }

        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            args.add(0, "cmd");
            args.add(1, "/c");
        }
        commandLine(args);

        getLogger().info("cli args: " + getArgs());

        super.exec();
    }
}
