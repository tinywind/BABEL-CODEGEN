# BABEL-CODEGEN
transform BABEL file into JS file

# configure maven

    <build>
        <plugins>
          <plugin>
            <groupId>com.tinywind</groupId>
            <artifactId>babel-codegen-maven</artifactId>
            <version>0.1-SNAPSHOT</version>
            <dependencies>
              <dependency>
                <groupId>com.tinywind</groupId>
                <artifactId>babel-codegen</artifactId>
                <version>0.1-SNAPSHOT</version>
              </dependency>
            </dependencies>
            <executions>
              <execution>
                <goals>
                  <goal>generate</goal>
                </goals>
              </execution>
            </executions>
            <configuration>
              <sources>
                  <source>
                    <sourceDir>${project.basedir}/src/main/resources/static/js</sourceDir>
                    <sourceEncoding>UTF-8</sourceEncoding>
                    <sourceFilePostfix>.jsx</sourceFilePostfix>
                    <targetDir>${project.basedir}/src/main/resources/static/js</targetDir>
                    <targetEncoding>UTF-8</targetEncoding>
                    <overwrite>true</overwrite>
                    <recursive>true</recursive>
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
