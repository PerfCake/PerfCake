/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.maven;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "integration")
public class ScenarioRunMojoTest extends AbstractMojoTestCase {

   private static final String POM_LOCATION;

   static {
      POM_LOCATION = Paths.get(ScenarioRunMojoTest.class.getProtectionDomain().getCodeSource().getLocation().getPath()).resolve("../..").normalize().toString();
   }

   @BeforeClass
   public void setUp() throws Exception {
      System.setProperty("basedir", POM_LOCATION);
      super.setUp();
   }

   @AfterClass
   public void tearDown() throws Exception {
      super.tearDown();
   }

//   @BeforeClass
   public void createPluginJar() throws IOException, InterruptedException {
      Files.copy(Paths.get("perfcake-maven-plugin/pom.xml"), Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/org.perfcake/perfcake-maven-plugin/pom.xml"), StandardCopyOption.REPLACE_EXISTING);
      Files.copy(Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/plugin.xml"), Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/plugin.xml.bak"), StandardCopyOption.REPLACE_EXISTING);
      final List<String> pluginLines = Files.lines(Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/plugin.xml")).collect(Collectors.toList());
      pluginLines.remove(9);
      pluginLines.add(8, "<version>1111-SNAPSHOT</version>");
      Files.write(Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/plugin.xml"), pluginLines);
      final Properties props = new Properties();
      props.put("groupId", "org.perfcake");
      props.put("artifactId", "perfcake-maven-plugin");
      props.put("version", "1111-SNAPSHOT");
      props.store(new FileOutputStream(new File("perfcake-maven-plugin/target/classes/META-INF/maven/org.perfcake/perfcake-maven-plugin/pom.properties")), null);
      new AddZip("perfcake-maven-plugin/target/classes", "perfcake-maven-plugin/target/tmp-plugin.jar").compress();
      runMaven("", "install:install-file", "-Dfile=perfcake-maven-plugin/target/tmp-plugin.jar", "-DgroupId=org.perfcake", "-DartifactId=perfcake-maven-plugin",
            "-Dversion=1111-SNAPSHOT", "-Dpackaging=jar");
   }

//   @AfterClass
   public void cleanUp() throws IOException, InterruptedException {
      runMaven(POM_LOCATION + "/pom.xml", "dependency:purge-local-repository", "-DmanualInclude=\"org.perfcake:perfcake-maven-plugin:1111-SNAPSHOT\"");
      Files.copy(Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/plugin.xml.bak"), Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/plugin.xml"), StandardCopyOption.REPLACE_EXISTING);
      Files.deleteIfExists(Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/plugin.xml.bak"));
      Files.deleteIfExists(Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/org.perfcake/perfcake-maven-plugin/pom.xml"));
      Files.deleteIfExists(Paths.get("perfcake-maven-plugin/target/classes/META-INF/maven/org.perfcake/perfcake-maven-plugin/pom.properties"));
   }

   @Test(enabled = false)
   public void testMavenPluginBasic() throws Exception {
      runMaven(POM_LOCATION + "/src/test/resources/projects/basic/pom.xml", "integration-test", "-X");
   }

   @Test
   public void testMojo() throws Exception {
      final File pom = getTestFile("/src/test/resources/projects/basic/pom.xml");
      System.out.println(pom.getAbsolutePath());
      assertNotNull(pom);
      assertTrue(pom.exists());
      final ScenarioRunMojo mojo = (ScenarioRunMojo) lookupMojo("scenario-run", pom);
      System.out.println(mojo.toString());
      mojo.execute();
   }

   private static void runMaven(final String pomFile, final String goal, final String... args) throws IOException, InterruptedException {
      String mavenExecutable;
      if (System.getProperty("os.name").toLowerCase().contains("windows")) {
         mavenExecutable = System.getenv("MAVEN_HOME") + File.separator + "bin" + File.separator + "mvn.bat";
      } else {
         mavenExecutable = System.getenv("MAVEN_HOME") + File.separator + "bin" + File.separator + "mvn";
      }

      final List<String> argsList = new ArrayList<>();
      argsList.add(mavenExecutable);
      argsList.add("-f");
      argsList.add(pomFile);
      argsList.add(goal);
      argsList.addAll(Arrays.asList(args));

      final ProcessBuilder pb = new ProcessBuilder().command(argsList).redirectErrorStream(true).redirectOutput(new File("maven-test-" + System.currentTimeMillis() + ".log"));
      pb.environment().put("plugin_location", new File("perfcake-maven-plugin/target/tmp-plugin.jar").getAbsolutePath());
      final Process p = pb.start();
      p.waitFor();
   }

}