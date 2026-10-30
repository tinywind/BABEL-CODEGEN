package org.tinywind.babelcodegen.gradle;

import org.gradle.api.Named;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;
import java.util.Collections;

public abstract class BabelSource implements Named {
    private final String name;

    @Inject
    public BabelSource(String name) {
        this.name = name;
        getSourceEncoding().convention("UTF-8");
        getTargetEncoding().convention("UTF-8");
        getSourceFilePostfix().convention(".jsx");
        getExcludes().convention(Collections.emptyList());
        getOverwrite().convention(true);
        getRecursive().convention(true);
    }

    @Override
    @Input
    public String getName() {
        return name;
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSourceDir();

    @OutputDirectory
    public abstract DirectoryProperty getTargetDir();

    @Input
    public abstract Property<String> getSourceEncoding();

    @Input
    public abstract Property<String> getTargetEncoding();

    @Input
    public abstract Property<String> getSourceFilePostfix();

    @Input
    public abstract ListProperty<String> getExcludes();

    @Input
    public abstract Property<Boolean> getOverwrite();

    @Input
    public abstract Property<Boolean> getRecursive();
}
