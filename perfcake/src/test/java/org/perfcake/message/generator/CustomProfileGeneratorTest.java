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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "integration")
public class CustomProfileGeneratorTest extends TestSetup {

   @Test
   public void testCustomProfileThreads() throws PerfCakeException, InterruptedException {
      System.setProperty("test.name", "threads");
      final Scenario scenario = ScenarioLoader.load("test-profile");
      final Thread t = getScenarioThread(scenario);

      final ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      final RunInfo runInfo = retractor.getReportManager().getRunInfo();

      int[] threadsWanted = new int[] { 10, 20, 30, 15, 10 };
      int[] noSonnerThan = new int[] { 0, 250, 500, 750, 900 };
      int pointer = 0;

      while (runInfo.getIteration() == -1) {
         Thread.sleep(10);
      }

      final ThreadPoolExecutor executor = ((CustomProfileGenerator) retractor.getGenerator()).executorService;

      while (runInfo.isRunning() && pointer < threadsWanted.length) {
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

      int[] speedWanted = new int[] { 100, 200, 100, 200 };
      int[] noSonnerThan = new int[] { 0, 250, 500, 750 };
      int pointer = 0;

      while (runInfo.getIteration() == -1) {
         Thread.sleep(10);
      }

      final CustomProfileGenerator generator = (CustomProfileGenerator) retractor.getGenerator();

      while (runInfo.isRunning() && pointer < speedWanted.length) {
         if (runInfo.getIteration() > noSonnerThan[pointer] && generator.getSpeed() == speedWanted[pointer]) {
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
            Assert.fail(e.toString());
         }
      });
      t.start();

      return t;
   }
}