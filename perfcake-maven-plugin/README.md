Maven plugin for PerfCake
=========================
Maven plugin which allows to run [PerfCake](http://www.perfcake.org) scenario within Maven build. This makes perfomance test
automation more easy and encourage you to run performance tests on a regular basis, e.g. in within your favourite CI server.
Running performance test on a regular basis allows you to spot performance drops very soon and thus make much more easy to
identify, which commit has caused performance regression.

Configuration
---
Currently plugin has only one goal `scenario-run`, which runs specified PerfCake scenario. By default, this goal is executed
in `integration-test` phase (i.e. assumes, that you deploy/start your application in `pre-integration-test` phase and 
shut it down in `post-integration-test` phase). 

The only mandatory parameter is `<scenario>`, which specifies the name of the scenario to be run. 
Optionally, you can also specify `scenarios-dir`, `messages-dir` and `plugins-dir`, which specify paths to
directory containing scenarios, messages and plugins, respectively. If you don't setup these parameters, plugin assumes,
that appropriate directories (`scenarios`, `messages`, `plugins`) are in `src/test/resources/perfcake`. This can be switched to
`src/main/resources/perfcake` by configuring `use-test-resources` to `false`. If any of these directories does not
exists, plugin will use `src/test/resources` (or `src/main/resources`) as a fallback value for missing parameter.

PerfCake logging level can be configured with `<log-level>` tag and additional properties file can be specified by `<properties-file>`.

The PerfCake Maven plugin requires log4j2 configuration file for PerfCake to report properly. By default it assumes
the file to be located in project root and named log4j2.xml. This can be changed by the `<log4j2-config>` tag.

The PerfCake version used with the plugin is always the same as plugin version. Both jar files are released together.

Example configuration
---

```xml
   <properties>
      <perfcake.version>7.0</perfcake.version>
      ...
   </properties>
   ...
   <dependencies>
      <dependency>
         <groupId>org.perfcake</groupId>
         <artifactId>perfcake</artifactId>
         <version>${perfcake.version}</version>
         <scope>test</scope>
      </dependency>
      ...
   </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.perfcake.maven</groupId>
        <artifactId>perfcake-maven-plugin</artifactId>
        <version>${perfcake.version}</version>
        <configuration>
          <scenario>my_perfcake_scenario</scenario>
          <log4j2-config>src/test/resources/log4j2.xml</log4j2-config>
        </configuration>
        <executions>
          <execution>
            <id>perfcake-scenario-run</id>
            <goals>
              <goal>scenario-run</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      ...
    </plugins>
  </build>
```
