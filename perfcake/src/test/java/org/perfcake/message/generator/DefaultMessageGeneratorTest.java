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

import org.perfcake.TestSetup;
import org.perfcake.message.sender.TestSender;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test @{DefaultMessageGenerator} features.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class DefaultMessageGeneratorTest extends TestSetup {

   @Test
   public void shutdownPeriodAutoTuneEnabledTest() throws Exception {
      final Scenario scenario;

      TestSender.resetCounter();

      System.setProperty("auto.tune", "-1");
      scenario = ScenarioLoader.load("test-shutdown-period-auto-tune-scenario");
      scenario.init();
      scenario.run();
      Thread.sleep(500);
      scenario.close();
      System.getProperties().remove("auto.tune");
      Assert.assertEquals(TestSender.getCounter(), 20);

      final ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      final MessageGenerator generator = retractor.getGenerator();
      Assert.assertTrue(generator instanceof DefaultMessageGenerator, "DefaultMessageGenerator");
      Assert.assertTrue(((DefaultMessageGenerator) generator).getShutdownPeriod() > 24999, "Auto tuned shuthown period.");
   }

   @Test
   public void shutdownPeriodAutoTuneDisabledTest() throws Exception {
      final Scenario scenario;

      TestSender.resetCounter();
      System.setProperty("auto.tune", "1000");
      scenario = ScenarioLoader.load("test-shutdown-period-auto-tune-scenario");
      scenario.init();
      scenario.run();
      Thread.sleep(500);
      scenario.close();
      System.getProperties().remove("auto.tune");

      Assert.assertEquals(TestSender.getCounter(), 20);

      final ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      final MessageGenerator generator = retractor.getGenerator();
      Assert.assertTrue(generator instanceof DefaultMessageGenerator, "DefaultMessageGenerator");
      Assert.assertEquals(((DefaultMessageGenerator) generator).getShutdownPeriod(), 1000, "Auto tuned shuthown period.");
   }
}
