package org.tinywind.babelcodegen.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tinywind.babelcodegen.jaxb.BabelOptions;
import org.tinywind.babelcodegen.jaxb.BabelPresets;
import org.tinywind.babelcodegen.jaxb.Source;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class PluginTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder(new File("target"));

    @Test
    public void failsTheMavenGoalWhenCompilationFails() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.write(input.resolve("broken.jsx"), "const value = ;".getBytes(StandardCharsets.UTF_8));
        Plugin plugin = new Plugin();
        set(plugin, "sources", Collections.singletonList(new Source().withSourceDir(input.toString())
                .withTargetDir(output.toString())));
        set(plugin, "babelOptions", new BabelOptions().withPresets(BabelPresets.REACT));
        MojoExecutionException failure = assertThrows(MojoExecutionException.class, plugin::execute);
        assertTrue(failure.getMessage(), failure.getMessage().contains("broken.jsx"));
    }

    @Test
    public void skipDoesNotRequireAValidCompilerConfiguration() throws Exception {
        Plugin plugin = new Plugin();
        set(plugin, "skip", true);
        plugin.execute();
    }

    private void set(Plugin plugin, String name, Object value) throws Exception {
        Field field = Plugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }
}
