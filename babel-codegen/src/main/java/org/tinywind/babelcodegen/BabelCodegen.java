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

import org.tinywind.babelcodegen.jaxb.BabelPresetsAdapter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static java.lang.System.exit;

/**
 * @author tinywind
 */
public class BabelCodegen {
    public final static String BABEL_CODEGEN_XSD = "babel-codegen-0.1.xsd";
    public final static String REPO_XSD_URL = "https://raw.githubusercontent.com/tinywind/BABEL-CODEGEN/master/babel-codegen/src/main/resources/xsd/";
    public final static String REPO_BABEL_CODEGEN_XSD = REPO_XSD_URL + BABEL_CODEGEN_XSD;
    private final SimpleBindings bindings = new SimpleBindings();
    private final ScriptEngine engine;

    public BabelCodegen() throws ScriptException, IOException {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("asset/babel.min.js")), bindings);
    }

    private static boolean isCorrected(Configuration configuration) {
        if (configuration.getSources() == null || configuration.getSources().size() == 0)
            return false;

        for (Source source : configuration.getSources()) {
            final String sourceDir = source.getSourceDir();
            if (sourceDir == null)
                return false;
        }

        return true;
    }

    private static void setDefault(Configuration configuration) {
        if (configuration.getSources() == null)
            configuration.setSources(new ArrayList<>());

        for (Source source : configuration.getSources()) {
            final String sourceDir = source.getSourceDir();
            source.setSourceDir(sourceDir == null ? "" : sourceDir.trim());

            final String targetDir = source.getTargetDir();
            source.setTargetDir(targetDir == null ? source.getSourceDir() : targetDir.trim());

            final String sourceEncoding = source.getSourceEncoding();
            source.setSourceEncoding(sourceEncoding == null ? "UTF-8" : sourceEncoding.trim());

            final String targetEncoding = source.getTargetEncoding();
            source.setTargetEncoding(targetEncoding == null ? "UTF-8" : targetEncoding.trim());

            final String filePostfix = source.getSourceFilePostfix();
            source.setSourceFilePostfix(filePostfix == null ? ".jsx" : filePostfix.trim());

            final String excludes = source.getExcludes();
            source.setExcludes(excludes == null ? "" : excludes.trim());

            final Boolean overwrite = source.isOverwrite();
            source.setOverwrite(overwrite == null ? true : overwrite);

            final Boolean recursive = source.isRecursive();
            source.setOverwrite(recursive == null ? true : recursive);
        }

        if (configuration.getBabelOptions() == null)
            configuration.setBabelOptions(new BabelOptions());
        final BabelOptions babelOptions = configuration.getBabelOptions();

        final BabelPresets presets = babelOptions.getPresets();
        babelOptions.setPresets(presets == null ? BabelPresets.REACT : presets);

        final Boolean minified = babelOptions.isMinified();
        babelOptions.setMinified(minified == null ? false : minified);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage : BabelCodegen <configuration-file>");
            exit(-1);
        }

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
                    continue;
                }

                System.out.println("Initialising properties: " + arg);

                final Configuration configuration = load(in);
                generate(configuration);
            } catch (Exception e) {
                System.err.println("Cannot read " + arg + ". Error : " + e.getMessage());
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
        System.out.println("complete transform JSX -> JS");
    }

    private static Configuration load(InputStream in) throws IOException {
        final byte[] buffer = new byte[1000 * 1000];
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int len; (len = in.read(buffer)) >= 0; )
            out.write(buffer, 0, len);

        final String xml = out.toString()
                .replaceAll("<(\\w+:)?configuration\\s+xmlns(:\\w+)?=\"[^\"]*\"[^>]*>", "<$1configuration xmlns$2=\"" + REPO_BABEL_CODEGEN_XSD + "\">")
                .replace("<configuration>", "<configuration xmlns=\"" + REPO_BABEL_CODEGEN_XSD + "\">");
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final JAXBContext ctx = JAXBContext.newInstance(Configuration.class);
            final Unmarshaller unmarshaller = ctx.createUnmarshaller();
            unmarshaller.setSchema(sf.newSchema(BabelCodegen.class.getResource("/xsd/" + BABEL_CODEGEN_XSD)));
            unmarshaller.setEventHandler(event -> true);
            return (Configuration) unmarshaller.unmarshal(new StringReader(xml));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void generate(Configuration configuration) throws ScriptException, IOException {
        if (!isCorrected(configuration)) {
            System.err.println("Incorrect xml");
            return;
        }
        setDefault(configuration);

        final BabelCodegen codegen = new BabelCodegen();
        configuration.getSources().forEach(source -> codegen.generate(new File(source.getSourceDir()), "", source, configuration.getBabelOptions()));
    }

    private void generate(File sourceDir, String subDir, Source source, BabelOptions babelOptions) {
        if (!sourceDir.exists())
            sourceDir = new File(sourceDir.getAbsolutePath());

        final File[] childFiles = sourceDir.listFiles();
        if (childFiles == null)
            return;

        final String dir = source.getTargetDir();
        final File targetDir = new File(new File(dir
                + (dir.length() > 0 && dir.length() - 1 != dir.lastIndexOf(File.separatorChar) ? File.separatorChar : "")
                + subDir).getAbsolutePath());

        if (!targetDir.mkdirs() && !targetDir.isDirectory()) {
            System.err.println(targetDir.getAbsolutePath() + " is not directory.");
            return;
        }

        for (File file : childFiles) {
            if (file.isDirectory()) {
                if (source.isRecursive())
                    generate(file, subDir + (subDir.length() > 0 ? File.separatorChar : "") + file.getName(), source, babelOptions);
                continue;
            }

            if (file.exists() && !source.isOverwrite())
                continue;

            final String fileName = file.getName();
            final String postfix = source.getSourceFilePostfix();
            if (fileName.length() <= postfix.length() || fileName.toLowerCase().lastIndexOf(postfix) != fileName.length() - postfix.length())
                continue;

            final File targetFile = new File(targetDir, fileName.substring(0, fileName.toLowerCase().lastIndexOf(postfix)) + ".js");
            if (targetFile.exists())
                if (!source.isOverwrite() || targetFile.isDirectory())
                    continue;

            try {
                final String originCode = new String(Files.readAllBytes(Paths.get(file.toURI())), source.getSourceEncoding());
                bindings.put("input", originCode);
                final String output = (String) engine.eval("Babel.transform(input, { presets: ['" + BabelPresetsAdapter.toString(babelOptions.getPresets()) + "'], minified: " + babelOptions.isMinified() + " }).code", bindings);
                Files.write(Paths.get(targetFile.toURI()), output.getBytes(source.getTargetEncoding()));
                System.out.println("   transformed: " + file.getAbsolutePath() + " -> " + targetFile.getAbsolutePath());
            } catch (IOException | ScriptException e) {
                e.printStackTrace();
            }
        }
    }
}
