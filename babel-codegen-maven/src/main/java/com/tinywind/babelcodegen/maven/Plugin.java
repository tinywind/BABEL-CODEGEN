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
package com.tinywind.babelcodegen.maven;

import com.tinywind.babelcodegen.BabelCodegen;
import com.tinywind.babelcodegen.jaxb.BabelOptions;
import com.tinywind.babelcodegen.jaxb.Configuration;
import com.tinywind.babelcodegen.jaxb.Source;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.script.ScriptException;
import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

/**
 * @author tinywind
 */
@Mojo(name = "generate", defaultPhase = GENERATE_SOURCES, requiresDependencyResolution = TEST)
public class Plugin extends AbstractMojo {
    /**
     * The Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    /**
     * Whether to skip the execution of the Maven Plugin for this module.
     */
    @Parameter
    private boolean skip;

    @Parameter
    private List<Source> sources;

    @Parameter
    private BabelOptions babelOptions;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skip BABEL-CODEGEN");
            return;
        }

        final Configuration configuration = new Configuration();
        configuration.setSources(sources);
        configuration.setBabelOptions(babelOptions);

        final StringWriter writer = new StringWriter();
        JAXB.marshal(configuration, writer);

        getLog().debug("Using this configuration:\n" + writer.toString());

        try {
            BabelCodegen.generate(configuration);
        } catch (ScriptException | IOException e) {
            e.printStackTrace();
            getLog().error(e.getMessage());
            if (e.getCause() != null)
                getLog().error("  Cause: " + e.getCause().getMessage());
            return;
        }

        getLog().info("Complete BABEL-CODEGEN");
    }
}
