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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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

   @Test
   public void testScenarioRunBasic() throws Exception {
      runProject("basic");
      test();
   }

   @Test
   public void testScenarioRunCustom() throws Exception {
      runProject("custom");
      test();
   }

   @Test
   public void testScenarioRunMain() throws Exception {
      runProject("main");
      test();
   }

   private void runProject(final String projectName) throws Exception {
      final File pom = getTestFile("/src/test/resources/projects/" + projectName + "/pom.xml");

      assertNotNull(pom);
      assertTrue(pom.exists());

      final ScenarioRunMojo mojo = (ScenarioRunMojo) lookupMojo("scenario-run", pom);
      mojo.testResourcesPath = getTestPath("/src/test/resources/projects/" + projectName + "/src/test/resources");
      mojo.resourcesPath = getTestPath("/src/test/resources/projects/" + projectName + "/src/main/resources");
      mojo.execute();
   }

   private void test() throws IOException {
      assertTrue(new File("perfcake-maven-plugin.log").exists());
      assertTrue(new File("perfcake-maven-plugin.csv").exists());
      assertTrue(Files.size(Paths.get("perfcake-maven-plugin.log")) > 0);
      assertTrue(Files.size(Paths.get("perfcake-maven-plugin.csv")) > 0);

      Files.deleteIfExists(Paths.get("perfcake-maven-plugin.log"));
      Files.deleteIfExists(Paths.get("perfcake-maven-plugin.csv"));
   }
}