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

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "integration")
public class ScenarioRunMojoTest {

   private static final String POM_LOCATION;

   static {
      POM_LOCATION = Paths.get(ScenarioRunMojoTest.class.getProtectionDomain().getCodeSource().getLocation().getPath()).resolve("../..").normalize().toString();
   }

   @Test(enabled = false)
   public void testMavenPluginBasic() throws Exception {
      runMaven(POM_LOCATION + "/src/test/resources/perfcake-basic-pom.xml", "test");
   }

   private void runMaven(final String pomFile, final String goal) throws IOException, InterruptedException {
      String mavenExecutable;
      if (System.getProperty("os.name").toLowerCase().contains("windows")) {
         mavenExecutable = System.getenv("MAVEN_HOME") + File.separator + "bin" + File.separator + "mvn.bat";
      } else {
         mavenExecutable = System.getenv("MAVEN_HOME") + File.separator + "bin" + File.separator + "mvn";
      }

      final Process p = new ProcessBuilder().command(mavenExecutable, "-f", pomFile, goal).redirectErrorStream(true).redirectOutput(new File("maven-test.log")).start();
      p.waitFor();
   }

}