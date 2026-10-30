package org.tinywind.babelcodegen.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

public class BabelCodegenPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        BabelCodegenExtension extension = project.getExtensions().create("babelCodegen", BabelCodegenExtension.class);
        TaskProvider<BabelCodegenTask> generator = project.getTasks().register("babelCodegen", BabelCodegenTask.class, task -> {
            task.setGroup("build");
            task.setDescription("Compile JavaScript and JSX with embedded Babel.");
            task.getPreset().convention(extension.getPreset());
            task.getMinified().convention(extension.getMinified());
            task.getSkip().convention(extension.getSkip());
        });
        extension.getSources().all(source -> {
            source.getTargetDir().convention(project.getLayout().getBuildDirectory().dir("generated/babel/" + source.getName()));
            generator.configure(task -> task.getSources().add(source));
        });
    }
}
