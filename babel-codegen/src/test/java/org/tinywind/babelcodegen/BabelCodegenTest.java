package org.tinywind.babelcodegen;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tinywind.babelcodegen.jaxb.BabelOptions;
import org.tinywind.babelcodegen.jaxb.BabelPresets;
import org.tinywind.babelcodegen.jaxb.Configuration;
import org.tinywind.babelcodegen.jaxb.Source;

import org.graalvm.polyglot.Context;
import javax.xml.bind.JAXB;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class BabelCodegenTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder(new File("target"));

    @Test
    public void transformsReactToMinifiedJavascript() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.write(input.resolve("sample.jsx"), "const element = <h1>한글</h1>;".getBytes(StandardCharsets.UTF_8));
        BabelCodegen.generate(configuration(input, output, BabelPresets.REACT, true, true, true));
        String compiled = read(output.resolve("sample.js"));
        assertTrue(compiled, compiled.contains("React.createElement"));
        assertFalse(compiled, compiled.contains("<h1>"));
        assertFalse(compiled, compiled.contains("\n"));
    }

    @Test
    public void transformsEs2015ToExecutableJavascript() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.write(input.resolve("sample.jsx"), "const label = \"한글\"; const double = value => value * 2;".getBytes(StandardCharsets.UTF_8));
        BabelCodegen.generate(configuration(input, output, BabelPresets.ES_2015, true, true, false));
        String compiled = read(output.resolve("sample.js"));
        assertFalse(compiled, compiled.contains("=>"));
        try (Context runtime = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
            assertEquals(8, runtime.eval("js", compiled + "; double(4);").asInt());
            assertEquals("한글", runtime.eval("js", "label").asString());
        }
    }

    @Test
    public void recursivelyCompilesNestedFiles() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.createDirectories(input.resolve("nested"));
        Files.write(input.resolve("nested/sample.jsx"), "const value = 1;".getBytes(StandardCharsets.UTF_8));
        BabelCodegen.generate(configuration(input, output, BabelPresets.ES_2015, true, true, false));
        assertTrue(Files.isRegularFile(output.resolve("nested/sample.js")));
    }

    @Test
    public void nonRecursiveGenerationStillCompilesTopLevelFiles() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.createDirectories(input.resolve("nested"));
        Files.write(input.resolve("sample.jsx"), "const value = 1;".getBytes(StandardCharsets.UTF_8));
        Files.write(input.resolve("nested/sample.jsx"), "const value = 2;".getBytes(StandardCharsets.UTF_8));
        BabelCodegen.generate(configuration(input, output, BabelPresets.ES_2015, true, false, false));
        assertTrue(Files.isRegularFile(output.resolve("sample.js")));
        assertFalse(Files.exists(output.resolve("nested/sample.js")));
    }

    @Test
    public void preservesExistingTargetsAndGeneratesMissingTargetsWhenOverwriteIsFalse() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.write(input.resolve("existing.jsx"), "const value = 1;".getBytes(StandardCharsets.UTF_8));
        Files.write(input.resolve("new.jsx"), "const value = 2;".getBytes(StandardCharsets.UTF_8));
        Files.write(output.resolve("existing.js"), "preserved-content".getBytes(StandardCharsets.UTF_8));
        BabelCodegen.generate(configuration(input, output, BabelPresets.ES_2015, false, true, false));
        assertEquals("preserved-content", read(output.resolve("existing.js")));
        assertTrue(Files.isRegularFile(output.resolve("new.js")));
    }

    @Test
    public void preservesXmlNamespaceFluentConfigurationAndSystemPropertySubstitution() throws Exception {
        String key = "babel.codegen.test.directory";
        String previous = System.getProperty(key);
        System.setProperty(key, "/configured/input");
        try {
            String xml = "<configuration xmlns=\"" + BabelCodegen.REPO_BABEL_CODEGEN_XSD + "\">"
                    + "<sources><sources><sourceDir>  ${" + key + "}  </sourceDir>"
                    + "<targetDir>${babel.codegen.test.missing}</targetDir></sources></sources>"
                    + "<babelOptions><presets>ES_2015</presets></babelOptions></configuration>";
            Configuration loaded = JAXB.unmarshal(new StringReader(xml), Configuration.class);
            assertEquals("/configured/input", loaded.getSources().get(0).getSourceDir());
            assertEquals("${babel.codegen.test.missing}", loaded.getSources().get(0).getTargetDir());
            Configuration fluent = new Configuration().withSources(new Source().withSourceDir("  /fluent/input  "))
                    .withBabelOptions(new BabelOptions().withPresets(BabelPresets.REACT));
            StringWriter serialized = new StringWriter();
            JAXB.marshal(fluent, serialized);
            Configuration roundTrip = JAXB.unmarshal(new StringReader(serialized.toString()), Configuration.class);
            assertEquals("/fluent/input", roundTrip.getSources().get(0).getSourceDir());
            assertEquals(BabelPresets.REACT, roundTrip.getBabelOptions().getPresets());
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }

    @Test
    public void failsOnInvalidJavascriptWithoutReplacingExistingOutput() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.write(input.resolve("broken.jsx"), "const value = ;".getBytes(StandardCharsets.UTF_8));
        Files.write(output.resolve("broken.js"), "preserved-content".getBytes(StandardCharsets.UTF_8));
        javax.script.ScriptException failure = assertThrows(javax.script.ScriptException.class,
                () -> BabelCodegen.generate(configuration(input, output, BabelPresets.REACT, true, true, false)));
        assertTrue(failure.getMessage(), failure.getMessage().contains("broken.jsx"));
        assertEquals("preserved-content", read(output.resolve("broken.js")));
    }

    @Test
    public void failsOnMissingInputDirectory() throws Exception {
        Path input = temporary.getRoot().toPath().resolve("missing-input");
        Path output = temporary.newFolder("output").toPath();
        IOException failure = assertThrows(IOException.class,
                () -> BabelCodegen.generate(configuration(input, output, BabelPresets.REACT, true, true, false)));
        assertTrue(failure.getMessage(), failure.getMessage().contains(input.toString()));
    }

    @Test
    public void failsOnInvalidConfiguration() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> BabelCodegen.generate(new Configuration()));
        Path output = temporary.newFolder("output").toPath();
        Configuration blank = configuration(output, output, BabelPresets.REACT, true, true, false);
        blank.getSources().get(0).setSourceDir("  ");
        assertThrows(IllegalArgumentException.class, () -> BabelCodegen.generate(blank));
    }

    @Test
    public void failsWhenTargetDirectoryIsAFile() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFile("output-file").toPath();
        Files.write(input.resolve("sample.jsx"), "const value = 1;".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class,
                () -> BabelCodegen.generate(configuration(input, output, BabelPresets.REACT, true, true, false)));
    }

    @Test
    public void excludesRootAndNestedFilesUsingCommaSeparatedAntPatterns() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.createDirectories(input.resolve("nested"));
        Files.createDirectories(input.resolve("vendor"));
        for (String name : new String[]{"ignored.jsx", "nested/ignored.jsx", "vendor/broken.jsx", "private.skip.jsx"}) {
            Files.write(input.resolve(name), "const value = ;".getBytes(StandardCharsets.UTF_8));
        }
        Files.write(input.resolve("included.jsx"), "const included = 1;".getBytes(StandardCharsets.UTF_8));
        Files.write(input.resolve("nested/included.jsx"), "const included = 2;".getBytes(StandardCharsets.UTF_8));
        Configuration configuration = configuration(input, output, BabelPresets.REACT, true, true, false);
        configuration.getSources().get(0).setExcludes(" **/ignored.jsx, vendor/**, **/*.skip.jsx ");
        BabelCodegen.generate(configuration);
        assertTrue(Files.isRegularFile(output.resolve("included.js")));
        assertTrue(Files.isRegularFile(output.resolve("nested/included.js")));
        assertFalse(Files.exists(output.resolve("ignored.js")));
        assertFalse(Files.exists(output.resolve("nested/ignored.js")));
        assertFalse(Files.exists(output.resolve("vendor")));
        assertFalse(Files.exists(output.resolve("private.skip.js")));
    }

    @Test
    public void nonRecursiveGenerationAlsoHonorsExcludes() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.write(input.resolve("ignored.jsx"), "const ignored = 1;".getBytes(StandardCharsets.UTF_8));
        Files.write(input.resolve("included.jsx"), "const included = 2;".getBytes(StandardCharsets.UTF_8));
        Configuration configuration = configuration(input, output, BabelPresets.REACT, true, false, false);
        configuration.getSources().get(0).setExcludes("**/ignored.jsx");
        BabelCodegen.generate(configuration);
        assertFalse(Files.exists(output.resolve("ignored.js")));
        assertTrue(Files.isRegularFile(output.resolve("included.js")));
    }

    @Test
    public void cliReturnsNonzeroForInvalidJavascript() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.write(input.resolve("broken.jsx"), "const value = ;".getBytes(StandardCharsets.UTF_8));
        File configurationFile = temporary.newFile("configuration.xml");
        JAXB.marshal(configuration(input, output, BabelPresets.REACT, true, true, false), configurationFile);
        File log = temporary.newFile("cli.log");
        Process process = new ProcessBuilder(new File(System.getProperty("java.home"), "bin/java").getPath(),
                "-cp", System.getProperty("java.class.path"), BabelCodegen.class.getName(), configurationFile.getPath())
                .redirectErrorStream(true).redirectOutput(log).start();
        if (!process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("CLI did not terminate");
        }
        String diagnostics = read(log.toPath());
        assertTrue(diagnostics, diagnostics.contains("broken.jsx"));
        assertTrue(diagnostics, process.exitValue() != 0);
        assertFalse(Files.exists(output.resolve("broken.js")));
    }

    @Test
    public void cliRejectsSchemaValidationErrors() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path output = temporary.newFolder("output").toPath();
        Files.write(input.resolve("sample.jsx"), "const value = 1;".getBytes(StandardCharsets.UTF_8));
        StringWriter xml = new StringWriter();
        JAXB.marshal(configuration(input, output, BabelPresets.REACT, true, true, false), xml);
        File configurationFile = temporary.newFile("invalid-configuration.xml");
        Files.write(configurationFile.toPath(), xml.toString().replace("<overwrite>true</overwrite>",
                "<overwrite>true</overwrite><unexpected/>").getBytes(StandardCharsets.UTF_8));
        File log = temporary.newFile("cli.log");
        Process process = new ProcessBuilder(new File(System.getProperty("java.home"), "bin/java").getPath(),
                "-cp", System.getProperty("java.class.path"), BabelCodegen.class.getName(), configurationFile.getPath())
                .redirectErrorStream(true).redirectOutput(log).start();
        if (!process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("CLI did not terminate");
        }
        String diagnostics = read(log.toPath());
        assertTrue(diagnostics, diagnostics.contains("unexpected"));
        assertTrue(diagnostics, process.exitValue() != 0);
        assertFalse(Files.exists(output.resolve("sample.js")));
    }

    private Configuration configuration(Path input, Path output, BabelPresets preset,
                                        boolean overwrite, boolean recursive, boolean minified) {
        Source source = new Source();
        source.setSourceDir(input.toString());
        source.setTargetDir(output.toString());
        source.setSourceEncoding("UTF-8");
        source.setTargetEncoding("UTF-8");
        source.setSourceFilePostfix(".jsx");
        source.setExcludes("");
        source.setOverwrite(overwrite);
        source.setRecursive(recursive);
        BabelOptions options = new BabelOptions();
        options.setPresets(preset);
        options.setMinified(minified);
        Configuration configuration = new Configuration();
        configuration.setSources(Collections.singletonList(source));
        configuration.setBabelOptions(options);
        return configuration;
    }

    private String read(Path file) throws Exception {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
