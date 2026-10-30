package org.tinywind.babelcodegen.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.tinywind.babelcodegen.BabelCodegen;
import org.tinywind.babelcodegen.jaxb.BabelOptions;
import org.tinywind.babelcodegen.jaxb.BabelPresets;
import org.tinywind.babelcodegen.jaxb.Configuration;
import org.tinywind.babelcodegen.jaxb.Source;

import javax.inject.Inject;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@DisableCachingByDefault(because = "Existing output can be preserved when overwrite is disabled")
public abstract class BabelCodegenTask extends DefaultTask {
    @Inject
    public BabelCodegenTask() {
        getSources().convention(Collections.emptyList());
        onlyIf("Babel generation is enabled", task -> !((BabelCodegenTask) task).getSkip().get());
    }

    @Nested
    public abstract ListProperty<BabelSource> getSources();

    @Input
    public abstract Property<String> getPreset();

    @Input
    public abstract Property<Boolean> getMinified();

    @Input
    public abstract Property<Boolean> getSkip();

    @TaskAction
    public void generate() {
        List<Source> sources = new ArrayList<>();
        for (BabelSource input : getSources().get()) {
            Source source = new Source();
            source.setSourceDir(input.getSourceDir().get().getAsFile().getAbsolutePath());
            source.setTargetDir(input.getTargetDir().get().getAsFile().getAbsolutePath());
            source.setSourceEncoding(input.getSourceEncoding().get());
            source.setTargetEncoding(input.getTargetEncoding().get());
            source.setSourceFilePostfix(input.getSourceFilePostfix().get());
            source.setExcludes(String.join(",", input.getExcludes().get()));
            source.setOverwrite(input.getOverwrite().get());
            source.setRecursive(input.getRecursive().get());
            sources.add(source);
        }

        Configuration configuration = new Configuration();
        configuration.setSources(sources);
        try {
            BabelOptions options = new BabelOptions();
            options.setPresets(BabelPresets.valueOf(getPreset().get()));
            options.setMinified(getMinified().get());
            configuration.setBabelOptions(options);
            BabelCodegen.generate(configuration);
        } catch (ScriptException | IOException | IllegalArgumentException e) {
            throw new GradleException("BABEL-CODEGEN failed: " + e.getMessage(), e);
        }
    }
}
