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
package org.perfcake.message.generator;

import org.perfcake.PerfCakeException;
import org.perfcake.RunInfo;
import org.perfcake.TestSetup;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "integration")
public class CustomProfileGeneratorTest extends TestSetup {

   private static final Logger log = LogManager.getLogger(CustomProfileGeneratorTest.class);

   @Test
   public void testCustomProfileThreads() throws PerfCakeException, InterruptedException {
      System.setProperty("test.name", "threads");
      final Scenario scenario = ScenarioLoader.load("test-profile");
      final Thread t = getScenarioThread(scenario);

      final ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      final RunInfo runInfo = retractor.getReportManager().getRunInfo();

      double[] threadsWanted = new double[] { 10, 20, 30, 15, 10 };
      int[] noSonnerThan = new int[] { 0, 250, 500, 750, 900 };
      int pointer = 0;

      while (runInfo.getIteration() == -1 && t.isAlive()) {
         Thread.sleep(10);
      }

      Assert.assertTrue(t.isAlive(), "Scenario thread terminated unexpectedly.");

      final ThreadPoolExecutor executor = ((CustomProfileGenerator) retractor.getGenerator()).executorService;

      while (runInfo.isRunning() && pointer < threadsWanted.length && t.isAlive()) {
         if (runInfo.getIteration() > noSonnerThan[pointer] && executor.getActiveCount() == threadsWanted[pointer]) {
            pointer++;
         }
         Thread.sleep(10);
      }

      t.join();

      Assert.assertEquals(pointer, threadsWanted.length);
   }

   @Test
   public void testCustomProfileSpeed() throws PerfCakeException, InterruptedException {
      System.setProperty("test.name", "speed");
      final Scenario scenario = ScenarioLoader.load("test-profile");
      final Thread t = getScenarioThread(scenario);

      final ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      final RunInfo runInfo = retractor.getReportManager().getRunInfo();

      double[] speedWanted = new double[] { 100, 200, 100, 200 };
      int[] noSonnerThan = new int[] { 0, 250, 500, 750 };
      int pointer = 0;

      while (runInfo.getIteration() == -1 && t.isAlive()) {
         Thread.sleep(10);
      }

      Assert.assertTrue(t.isAlive(), "Scenario thread terminated unexpectedly.");

      final CustomProfileGenerator generator = (CustomProfileGenerator) retractor.getGenerator();

      while (runInfo.isRunning() && pointer < speedWanted.length && t.isAlive()) {
         if (runInfo.getIteration() > noSonnerThan[pointer] && generator.getSpeed() == speedWanted[pointer]) {
            pointer++;
         }
         Thread.sleep(10);
      }

      t.join();

      Assert.assertEquals(pointer, speedWanted.length);
   }

   @Test
   public void testCustomProfileSlow() throws PerfCakeException, InterruptedException {
      System.setProperty("test.name", "slow");
      final Scenario scenario = ScenarioLoader.load("test-profile-slow");
      final Thread t = getScenarioThread(scenario);

      final ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      final RunInfo runInfo = retractor.getReportManager().getRunInfo();

      double[] speedWanted = new double[] { 0.5, 0.5 };
      int[] noSonnerThan = new int[] { 0, 500 };
      int pointer = 0;

      while (runInfo.getIteration() == -1 && t.isAlive()) {
         Thread.sleep(10);
      }

      Assert.assertTrue(t.isAlive(), "Scenario thread terminated unexpectedly.");

      final CustomProfileGenerator generator = (CustomProfileGenerator) retractor.getGenerator();

      while (runInfo.isRunning() && pointer < speedWanted.length && t.isAlive()) {
         if (runInfo.getRunTime() > noSonnerThan[pointer] && generator.getSpeed() == speedWanted[pointer]) {
            pointer++;
         }
         Thread.sleep(10);
      }

      t.join();

      Assert.assertEquals(pointer, speedWanted.length);
   }

   private Thread getScenarioThread(final Scenario scenario) throws PerfCakeException {
      scenario.init();

      final Thread t = new Thread(() -> {
         try {
            scenario.run();
            scenario.close();
         } catch (PerfCakeException e) {
            log.error("Could not run scenario: ", e);
         }
      });
      t.start();

      return t;
   }
}