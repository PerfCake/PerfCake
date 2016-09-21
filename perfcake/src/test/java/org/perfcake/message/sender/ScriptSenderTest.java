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
package org.perfcake.message.sender;

import org.perfcake.TestSetup;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ScriptSenderTest extends TestSetup {
   private Scenario scenario;
   private String tmpPath;

   @BeforeMethod
   public void prepareScenario() throws Exception {
      tmpPath = createTempDir("ScenarioSenderTest");
      System.setProperty("tmpPath", tmpPath);
      scenario = ScenarioLoader.load("test-script-sender-scenario");
   }

   @Test
   public void fullScenarioExecutionTest() throws Exception {
      scenario.init();
      scenario.run();
      final File tmpDir = new File(tmpPath);
      Assert.assertTrue(tmpDir.exists());
      for (int i = 0; i < 100; i++) {
         File file = new File(tmpPath, "script-sender-message-" + i);
         Assert.assertTrue(file.exists());
         final BufferedReader br = new BufferedReader(new FileReader(file));
         String line = null;
         while ((line = br.readLine()) != null) {
            Assert.assertEquals(line, String.valueOf(i));
         }
         Assert.assertTrue(file.delete());
      }
      Assert.assertTrue(tmpDir.delete());
   }
}
