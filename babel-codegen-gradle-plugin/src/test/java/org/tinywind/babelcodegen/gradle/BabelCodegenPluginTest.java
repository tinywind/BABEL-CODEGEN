package org.tinywind.babelcodegen.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BabelCodegenPluginTest {
    @TempDir
    public Path temporary;

    @Test
    public void compilesExcludedInputsAndReusesConfigurationCache() throws Exception {
        Path project = project();
        write(project, "input/sample.jsx", "const element = <h1>한글</h1>;");
        write(project, "input/nested/sample.jsx", "const element = <h2>Nested</h2>;");
        write(project, "input/ignored.jsx", "const broken = ;");
        write(project, "input/nested/ignored.jsx", "const broken = ;");
        write(project, "input/vendor/broken.jsx", "const broken = ;");
        write(project, "build.gradle", configuration("REACT", "", false));
        BuildResult result = runner(project, "babelCodegen", "--configuration-cache").build();
        assertEquals(TaskOutcome.SUCCESS, result.task(":babelCodegen").getOutcome());
        assertTrue(result.getOutput().contains("BABEL_TEST_JAVA=" + System.getProperty("babelTestJavaVersion")), result.getOutput());
        System.out.println("Verified consumer Java " + System.getProperty("babelTestJavaVersion")
                + " on " + System.getProperty("babelTestGradleHome"));
        String compiled = Files.readString(project.resolve("output/sample.js"));
        assertTrue(compiled.contains("React.createElement"), compiled);
        assertTrue(compiled.contains("한글"), compiled);
        assertFalse(compiled.contains("react/jsx-runtime"), compiled);
        assertTrue(Files.isRegularFile(project.resolve("output/nested/sample.js")));
        assertFalse(Files.exists(project.resolve("output/ignored.js")));
        assertFalse(Files.exists(project.resolve("output/nested/ignored.js")));
        assertFalse(Files.exists(project.resolve("output/vendor")));
        BuildResult cached = runner(project, "babelCodegen", "--configuration-cache").build();
        assertEquals(TaskOutcome.UP_TO_DATE, cached.task(":babelCodegen").getOutcome());
        assertTrue(cached.getOutput().contains("Configuration cache entry reused"), cached.getOutput());
    }

    @Test
    public void failsForInvalidJavascriptAndPreservesExistingOutput() throws Exception {
        Path project = project();
        write(project, "input/broken.jsx", "const value = ;");
        write(project, "output/broken.js", "preserved-content");
        write(project, "build.gradle", configuration("REACT", "", false));
        BuildResult result = runner(project, "babelCodegen").buildAndFail();
        assertTrue(result.getOutput().contains("broken.jsx"), result.getOutput());
        assertEquals("preserved-content", Files.readString(project.resolve("output/broken.js")));
    }

    @Test
    public void skipDoesNotRequireAnExistingInputDirectory() throws Exception {
        Path project = project();
        write(project, "build.gradle", configuration("REACT", "", true));
        BuildResult result = runner(project, "babelCodegen").build();
        assertEquals(TaskOutcome.SKIPPED, result.task(":babelCodegen").getOutcome());
        assertFalse(Files.exists(project.resolve("output")));
    }

    @Test
    public void preservesOutputWhenOverwriteIsDisabled() throws Exception {
        Path project = project();
        write(project, "input/existing.jsx", "const value = 1;");
        write(project, "input/new.jsx", "const double = value => value * 2;");
        write(project, "output/existing.js", "preserved-content");
        write(project, "build.gradle", configuration("ES_2015", "overwrite.set(false)", false));
        runner(project, "babelCodegen").build();
        assertEquals("preserved-content", Files.readString(project.resolve("output/existing.js")));
        String compiled = Files.readString(project.resolve("output/new.js"));
        assertFalse(compiled.contains("=>"), compiled);
        assertTrue(compiled.contains("double"), compiled);
    }

    @Test
    public void supportsMultipleSourcesAndNonRecursiveGeneration() throws Exception {
        Path project = project();
        write(project, "input/sample.jsx", "const value = 1;");
        write(project, "input/nested/sample.jsx", "const value = 2;");
        write(project, "extra/sample.jsx", "const value = 3;");
        String build = configuration("ES_2015", "recursive.set(false)", false)
                + "\nbabelCodegen.sources.create('extra') { sourceDir.set(layout.projectDirectory.dir('extra')); targetDir.set(layout.projectDirectory.dir('extra-output')) }\n";
        write(project, "build.gradle", build);
        runner(project, "babelCodegen").build();
        assertTrue(Files.isRegularFile(project.resolve("output/sample.js")));
        assertFalse(Files.exists(project.resolve("output/nested/sample.js")));
        assertTrue(Files.isRegularFile(project.resolve("extra-output/sample.js")));
    }

    @Test
    public void supportsKotlinDslAndDefaultOutputDirectory() throws Exception {
        Path project = project();
        write(project, "input/sample.jsx", "const element = <h1>한글</h1>;");
        write(project, "build.gradle.kts", "plugins { id(\"org.tinywind.babel-codegen\") }\n"
                + "babelCodegen { preset.set(\"REACT\"); sources { create(\"main\") { "
                + "sourceDir.set(layout.projectDirectory.dir(\"input\")) } } }\n");
        runner(project, "babelCodegen").build();
        String compiled = Files.readString(project.resolve("build/generated/babel/main/sample.js"));
        assertTrue(compiled.contains("React.createElement"), compiled);
        assertTrue(compiled.contains("한글"), compiled);
    }

    @Test
    public void packagesGeneratedOutputInJavaResources() throws Exception {
        Path project = project();
        write(project, "input/sample.jsx", "const element = <h1>Packaged</h1>;");
        String build = configuration("REACT", "", false).replace("plugins {", "plugins { id 'java';")
                + "\ntasks.named('processResources') { from(tasks.named('babelCodegen')) { into 'static/js' } }\n";
        write(project, "build.gradle", build);
        BuildResult result = runner(project, "jar").build();
        assertEquals(TaskOutcome.SUCCESS, result.task(":babelCodegen").getOutcome());
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(project.resolve("build/libs/babel-consumer.jar").toFile())) {
            assertTrue(jar.getEntry("static/js/sample.js") != null);
        }
    }

    private Path project() throws Exception {
        Path project = Files.createDirectory(temporary.resolve("consumer")).toAbsolutePath();
        write(project, "settings.gradle", "rootProject.name = 'babel-consumer'\n"
                + "println('BABEL_TEST_JAVA=' + System.getProperty('java.specification.version'))\n");
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("org.gradle.jvmargs", "-Xmx768m");
        properties.setProperty("org.gradle.daemon", "false");
        properties.setProperty("org.gradle.java.home", System.getProperty("babelTestJavaHome"));
        try (java.io.OutputStream output = Files.newOutputStream(project.resolve("gradle.properties"))) {
            properties.store(output, null);
        }
        return project;
    }

    private String configuration(String preset, String sourceOptions, boolean skip) {
        return "plugins { id 'org.tinywind.babel-codegen' }\n"
                + "babelCodegen { preset.set('" + preset + "'); minified.set(false); skip.set(" + skip + "); sources { main { "
                + "sourceDir.set(layout.projectDirectory.dir('input')); targetDir.set(layout.projectDirectory.dir('output')); "
                + "excludes.set(['**/ignored.jsx', 'vendor/**']); " + sourceOptions + " } } }\n";
    }

    private GradleRunner runner(Path project, String... arguments) {
        return GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath()
                .withGradleInstallation(new File(System.getProperty("babelTestGradleHome")))
                .withTestKitDir(new File("build/test-kit").getAbsoluteFile()).withArguments(arguments);
    }

    private void write(Path project, String name, String content) throws Exception {
        Path file = project.resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }
}
