/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2016 Jeon JaeHyeong
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.tinywind.babelcodegen;

import org.tinywind.babelcodegen.jaxb.*;
import org.codehaus.plexus.util.SelectorUtils;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import javax.script.ScriptException;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import static java.lang.System.exit;

/**
 * @author tinywind
 */
public class BabelCodegen implements AutoCloseable {
    public final static String BABEL_CODEGEN_XSD = "babel-codegen-0.1.xsd";
    public final static String REPO_XSD_URL = "https://raw.githubusercontent.com/tinywind/BABEL-CODEGEN/master/babel-codegen/src/main/resources/xsd/";
    public final static String REPO_BABEL_CODEGEN_XSD = REPO_XSD_URL + BABEL_CODEGEN_XSD;
    private static final class CompilerRuntime {
        static final Engine ENGINE = Engine.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build();
        private static org.graalvm.polyglot.Source script;

        static synchronized org.graalvm.polyglot.Source script() throws IOException {
            if (script == null) {
                URL resource = BabelCodegen.class.getResource("/asset/babel.min.js");
                if (resource == null)
                    throw new IOException("Missing embedded compiler: asset/babel.min.js");
                script = org.graalvm.polyglot.Source.newBuilder("js", resource).name("babel.min.js")
                        .encoding(java.nio.charset.StandardCharsets.UTF_8).build();
            }
            return script;
        }
    }

    private final Context engine;
    private final Value transform;

    public BabelCodegen() throws ScriptException, IOException {
        engine = Context.newBuilder("js").engine(CompilerRuntime.ENGINE).allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(name -> false).build();
        try {
            engine.eval(CompilerRuntime.script());
            transform = engine.eval("js", "(input, filename, preset, minified) => Babel.transform(input, {"
                    + "filename, presets: preset === 'react' ? [['react', { runtime: 'classic' }]]"
                    + " : [['env', { targets: { ie: '11' }, modules: 'commonjs', ignoreBrowserslistConfig: true }]],"
                    + "minified }).code");
        } catch (PolyglotException e) {
            engine.close();
            ScriptException failure = new ScriptException("Cannot initialize embedded Babel: " + e.getMessage());
            failure.initCause(e);
            throw failure;
        } catch (IOException | RuntimeException e) {
            engine.close();
            throw e;
        }
    }

    @Override
    public void close() {
        engine.close();
    }

    private static boolean isCorrected(Configuration configuration) {
        if (configuration == null || configuration.getSources() == null || configuration.getSources().isEmpty())
            return false;

        for (Source source : configuration.getSources()) {
            if (source == null || source.getSourceDir() == null || source.getSourceDir().trim().isEmpty())
                return false;
        }

        return true;
    }

    private static String trimmedOrDefault(String value, String fallback) {
        return value == null ? fallback : value.trim();
    }

    private static void setDefault(Configuration configuration) {
        Source defaults = new Source();
        for (Source source : configuration.getSources()) {
            source.setSourceDir(source.getSourceDir().trim());
            source.setTargetDir(trimmedOrDefault(source.getTargetDir(), source.getSourceDir()));
            source.setSourceEncoding(trimmedOrDefault(source.getSourceEncoding(), defaults.getSourceEncoding()));
            source.setTargetEncoding(trimmedOrDefault(source.getTargetEncoding(), defaults.getTargetEncoding()));
            source.setSourceFilePostfix(trimmedOrDefault(source.getSourceFilePostfix(), defaults.getSourceFilePostfix()));
            source.setExcludes(trimmedOrDefault(source.getExcludes(), defaults.getExcludes()));
            if (source.isOverwrite() == null)
                source.setOverwrite(defaults.isOverwrite());
            if (source.isRecursive() == null)
                source.setRecursive(defaults.isRecursive());
        }

        BabelOptions optionDefaults = new BabelOptions();
        if (configuration.getBabelOptions() == null)
            configuration.setBabelOptions(optionDefaults);
        BabelOptions options = configuration.getBabelOptions();
        if (options.getPresets() == null)
            options.setPresets(optionDefaults.getPresets());
        if (options.isMinified() == null)
            options.setMinified(optionDefaults.isMinified());
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage : " + BabelCodegen.class.getName() + " <configuration-file>");
            exit(-1);
        }

        boolean failed = false;
        for (String arg : args) {
            InputStream in = BabelCodegen.class.getResourceAsStream(arg);
            try {
                if (in == null && !arg.startsWith("/"))
                    in = BabelCodegen.class.getResourceAsStream("/" + arg);

                if (in == null && new File(arg).exists())
                    in = new FileInputStream(new File(arg));

                if (in == null) {
                    System.err.println("Cannot find " + arg + " on classpath, or in directory " + new File(".").getCanonicalPath());
                    System.err.println("-----------");
                    System.err.println("Please be sure it is located");
                    System.err.println("  - on the classpath and qualified as a classpath location.");
                    System.err.println("  - in the local directory or at a global path in the file system.");
                    failed = true;
                    continue;
                }

                System.out.println("Initialising properties: " + arg);

                final Configuration configuration = ConfigurationXml.load(in);
                generate(configuration);
            } catch (Exception e) {
                failed = true;
                System.err.println("Cannot process " + arg + ". Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (failed)
            exit(1);
        System.out.println("complete transform JSX -> JS");
    }

    public static void generate(Configuration configuration) throws ScriptException, IOException {
        if (!isCorrected(configuration))
            throw new IllegalArgumentException("Configuration must contain at least one nonblank source directory");
        setDefault(configuration);

        try (BabelCodegen codegen = new BabelCodegen()) {
            for (Source source : configuration.getSources())
                codegen.generate(new File(source.getSourceDir()), "", source, configuration.getBabelOptions());
        }
    }

    private static boolean isExcluded(String relativePath, String excludes) {
        for (String value : excludes.split(",")) {
            String pattern = value.trim().replace('\\', '/');
            if (pattern.isEmpty())
                continue;
            if (pattern.endsWith("/"))
                pattern += "**";
            if (SelectorUtils.matchPath(pattern, relativePath, "/", true))
                return true;
        }
        return false;
    }

    private void generate(File sourceDir, String subDir, Source source, BabelOptions babelOptions) throws IOException, ScriptException {
        final File[] childFiles = sourceDir.listFiles();
        if (childFiles == null)
            throw new IOException("Cannot read source directory: " + sourceDir.getAbsolutePath());

        final File targetDir = new File(source.getTargetDir(), subDir).getAbsoluteFile();
        if (!targetDir.mkdirs() && !targetDir.isDirectory())
            throw new IOException("Cannot create target directory: " + targetDir.getAbsolutePath());

        for (File file : childFiles) {
            final String relativePath = subDir.isEmpty() ? file.getName()
                    : subDir.replace(File.separatorChar, '/') + "/" + file.getName();
            if (isExcluded(relativePath, source.getExcludes()))
                continue;
            if (file.isDirectory()) {
                if (source.isRecursive())
                    generate(file, relativePath.replace('/', File.separatorChar), source, babelOptions);
                continue;
            }

            final String fileName = file.getName();
            final String postfix = source.getSourceFilePostfix();
            if (fileName.length() <= postfix.length() || !fileName.toLowerCase(Locale.ROOT).endsWith(postfix.toLowerCase(Locale.ROOT)))
                continue;

            final File targetFile = new File(targetDir, fileName.substring(0, fileName.length() - postfix.length()) + ".js");
            if (targetFile.isDirectory())
                throw new IOException("Target file is a directory: " + targetFile.getAbsolutePath());
            if (targetFile.exists() && !source.isOverwrite())
                continue;

            try {
                final String originCode = new String(Files.readAllBytes(Paths.get(file.toURI())), source.getSourceEncoding());
                final String output = transform.execute(originCode, file.getAbsolutePath(),
                        BabelPresetsAdapter.toString(babelOptions.getPresets()), babelOptions.isMinified()).asString();
                Files.write(Paths.get(targetFile.toURI()), output.getBytes(source.getTargetEncoding()));
                System.out.println("   transformed: " + file.getAbsolutePath() + " -> " + targetFile.getAbsolutePath());
            } catch (PolyglotException e) {
                ScriptException failure = new ScriptException("Cannot transform " + file.getAbsolutePath() + ": " + e.getMessage());
                failure.initCause(e);
                throw failure;
            } catch (IOException e) {
                throw new IOException("Cannot transform " + file.getAbsolutePath() + ": " + e.getMessage(), e);
            }
        }
    }
}
