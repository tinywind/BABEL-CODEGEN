# BABEL-CODEGEN

Compile JavaScript and JSX from Maven or Gradle with Babel 8 embedded in GraalJS. Both plugins run on Java 21 without a Node.js installation.

Version `0.2.1`. The Maven plugin metadata requires Maven 3.10.0. The Gradle plugin supports Java 21 builds on Gradle 8.6 and 9.8.2; Java 25 is tested with Gradle 9.8.2.

## Maven

Add the plugin and generated resource directory to your build:

```xml
<build>
  <resources>
    <resource><directory>src/main/resources</directory></resource>
    <resource><directory>${project.build.directory}/generated-resources</directory></resource>
  </resources>
  <plugins>
    <plugin>
      <groupId>org.tinywind</groupId>
      <artifactId>babel-codegen-maven</artifactId>
      <version>0.2.1</version>
      <executions>
        <execution>
          <phase>generate-resources</phase>
          <goals><goal>generate</goal></goals>
        </execution>
      </executions>
      <configuration>
        <sources>
          <source>
            <sourceDir>${project.basedir}/src/main/js</sourceDir>
            <targetDir>${project.build.directory}/generated-resources/static/js</targetDir>
            <excludes>**/ignored.jsx,vendor/**</excludes>
          </source>
        </sources>
        <babelOptions>
          <presets>REACT</presets>
          <minified>true</minified>
        </babelOptions>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Run `mvn generate-resources` with this configuration, or invoke the goal with `mvn babel-codegen:generate`. Set `<skip>true</skip>` in the plugin configuration to skip generation. Configure each source's input and output directories explicitly.

## Gradle

```groovy
plugins {
    id 'java'
    id 'org.tinywind.babel-codegen' version '0.2.1'
}

babelCodegen {
    preset.set('REACT')
    minified.set(true)
    sources {
        main {
            sourceDir.set(layout.projectDirectory.dir('src/main/js'))
            excludes.set(['**/ignored.jsx', 'vendor/**'])
        }
    }
}

tasks.named('processResources') {
    from(tasks.named('babelCodegen')) {
        into 'static/js'
    }
}
```

The `babelCodegen` task generates each named source into `build/generated/babel/<name>` by default. Set its `targetDir` to choose another directory. Add more named sources when you need multiple input roots. Set `babelCodegen.skip` to `true` to skip the task. The Java resource configuration above also makes generation a dependency of `processResources`.

The extension uses Gradle properties and supports Groovy and Kotlin DSL. For example, a Kotlin source declaration is:

```kotlin
babelCodegen {
    sources {
        create("main") {
            sourceDir.set(layout.projectDirectory.dir("src/main/js"))
            excludes.set(listOf("**/ignored.jsx", "vendor/**"))
        }
    }
}
```

## Options and failure behavior

Each source supports `sourceEncoding` and `targetEncoding` (default `UTF-8`), `sourceFilePostfix` (default `.jsx`), `recursive` (default `true`), and `overwrite` (default `true`). Exclusions use a comma-separated string in Maven and a list of strings in Gradle.

Exclusion patterns are case-sensitive Ant patterns relative to the input root. `**/ignored.jsx` matches root and nested files; `vendor/**` skips that directory before traversing or parsing it. `overwrite=false` preserves existing files and still generates missing files.

`REACT` preserves classic `React.createElement` output. `ES_2015` uses Babel's `env` preset with an IE 11 syntax target and CommonJS modules. Applications provide any required runtime polyfills. `minified` requests compact Babel output.

Invalid configuration, missing input directories, JavaScript syntax errors and output failures fail the build. A syntax error preserves that file's existing output. Other files already generated before a failure remain on disk. The CLI exits nonzero when configuration or generation fails.

Version 0.2.1 uses Jakarta XML Binding 4. Java callers that marshal the configuration model or implement XML adapters must use `jakarta.xml.bind` imports. Existing XML configuration files and Maven/Gradle options retain their format.

## Build and verify

Use Java 21. The Maven wrapper downloads the pinned Maven 3.10.0 distribution and checks its SHA-256:

```sh
./mvnw -B -ntp install
cd babel-codegen-gradle-plugin
./gradlew --no-daemon build
```

The Maven build runs compiler tests, Maven goal tests and isolated Maven consumer builds. The Gradle build runs real TestKit consumers, including Kotlin DSL, resource packaging, configuration-cache reuse, exclusions, overwrite and failure behavior.

See [DEPENDENCY_UPGRADE.md](DEPENDENCY_UPGRADE.md) for runtime versions, compatibility and publication details.

## License

[MIT](LICENSE.txt). The bundled Babel distribution retains its upstream license under `META-INF/licenses`.
