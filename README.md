# BABEL-CODEGEN
transform BABEL file into JS file

Requires JDK 17 and Maven 3. Run `mvn verify` to build both artifacts and run the six compiler/configuration contracts. Node.js is used for additional output verification; the compiler and Maven goal run entirely in the JVM. See [DEPENDENCY_UPGRADE.md](DEPENDENCY_UPGRADE.md) for compatibility and dependency details.

# configure maven
    <build>
        <plugins>
          <plugin>
            <groupId>org.tinywind</groupId>
            <artifactId>babel-codegen-maven</artifactId>
            <version>0.1.3</version>
            <executions>
              <execution>
                <phase>generate-sources</phase>
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

# historical result

The following output records the original Java 8 version. The current compiler requires Java 17.

    "C:\Program Files\Java\jdk1.8.0_73\bin\java" "-Dmaven.home=C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.1.1\plugins\maven\lib\maven3" "-Dclassworlds.conf=C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.1.1\plugins\maven\lib\maven3\bin\m2.conf" -Didea.launcher.port=7536 "-Didea.launcher.bin.path=C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.1.1\bin" -Dfile.encoding=UTF-8 -classpath "C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.1.1\plugins\maven\lib\maven3\boot\plexus-classworlds-2.4.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.1.1\lib\idea_rt.jar" com.intellij.rt.execution.application.AppMain org.codehaus.classworlds.Launcher -Didea.version=2016.1.3 org.tinywind:babel-codegen-maven:0.1.3:generate
    [INFO] Scanning for projects...
    [INFO]                                                                         
    [INFO] ------------------------------------------------------------------------
    [INFO] Building spring-boot-reactive 0.0.1-SNAPSHOT
    [INFO] ------------------------------------------------------------------------
    [INFO] 
    [INFO] --- babel-codegen-maven:0.1.3:generate (default-cli) @ reactive ---
       transformed: C:\Users\tinywind\IdeaProjects\tinywind-spring-boot-reactive\src\main\resources\static\js\doms.jsx -> C:\Users\tinywind\IdeaProjects\tinywind-spring-boot-reactive\src\main\resources\static\js\doms.js
       transformed: C:\Users\tinywind\IdeaProjects\tinywind-spring-boot-reactive\src\main\resources\static\js\playfield.jsx -> C:\Users\tinywind\IdeaProjects\tinywind-spring-boot-reactive\src\main\resources\static\js\playfield.js
    [INFO] Complete BABEL-CODEGEN
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 13.319s
    [INFO] Finished at: Sat Jun 25 15:02:39 KST 2016
    [INFO] Final Memory: 25M/619M
    [INFO] ------------------------------------------------------------------------
    
    Process finished with exit code 0

# LICENSE
**The MIT License (MIT)**
