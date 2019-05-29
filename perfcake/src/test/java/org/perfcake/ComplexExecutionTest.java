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
package org.perfcake;

import org.perfcake.message.sender.TestSender;
import org.perfcake.reporting.destination.Destination;
import org.perfcake.reporting.destination.DummyDestination;
import org.perfcake.reporting.reporter.DummyReporter;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test complete scenario execution.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ComplexExecutionTest extends TestSetup {

   @Test
   public void iterationScenarioTest() throws Exception {
      final Scenario scenario;

      TestSender.resetCounter();

      scenario = ScenarioLoader.load("test-iteration-scenario");
      scenario.init();
      scenario.run();
      Thread.sleep(500);
      scenario.close();

      Assert.assertEquals(TestSender.getCounter(), 1);

      final ScenarioRetractor sr = new ScenarioRetractor(scenario);
      final String scenarioName = sr.getReportManager().getRunInfo().getScenarioName();

      Assert.assertTrue(scenarioName.endsWith("scenarios/test-iteration-scenario.xml"));
      Assert.assertTrue(scenarioName.startsWith("file:"));
   }

   @Test
   public void failuresTest() throws Exception {
      final Scenario scenario;

      TestSender.resetCounter();

      scenario = ScenarioLoader.load("test-failures-scenario");
      scenario.init();
      scenario.run();
      Thread.sleep(500);
      scenario.close();

      ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      retractor.getReportManager().getReporters().forEach(r ->
            Assert.assertEquals(((DummyReporter) r).getLastFailures(), 10L)
      );

      Assert.assertEquals(TestSender.getCounter(), 20);
   }

   @Test
   public void slowServiceTest() throws Exception {
      final Scenario scenario;
      DummyDestination dd = null;

      TestSender.resetCounter();

      scenario = ScenarioLoader.load("test-long-processing");
      scenario.init();

      ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      for (Destination d : retractor.getReportManager().getReporters().iterator().next().getDestinations()) {
         if (d instanceof DummyDestination) {
            dd = (DummyDestination) d;
            break;
         }
      }
      Assert.assertNotNull(dd);
      dd.setObserving(true);

      scenario.run();
      Thread.sleep(500);
      scenario.close();

      dd.setObserving(false);
      Assert.assertEquals(dd.getObservedMeasurements().size(), 10);
   }

   @Test
   public void tooSlowServiceTest() throws Exception {
      final Scenario scenario;
      DummyDestination dd = null;

      TestSender.resetCounter();

      scenario = ScenarioLoader.load("test-very-long-processing");
      scenario.init();

      ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      for (Destination d : retractor.getReportManager().getReporters().iterator().next().getDestinations()) {
         if (d instanceof DummyDestination) {
            dd = (DummyDestination) d;
            break;
         }
      }
      Assert.assertNotNull(dd);
      dd.setObserving(true);

      scenario.run();
      scenario.close();

      dd.setObserving(false);
      System.out.println(dd.getObservedMeasurements().size());
      Assert.assertTrue(dd.getObservedMeasurements().size() < 30);
   }

}
