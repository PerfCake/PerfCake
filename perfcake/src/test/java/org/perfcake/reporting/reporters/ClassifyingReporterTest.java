/*
 * -----------------------------------------------------------------------\
 * SilverWare
 *  
 * Copyright (C) 2014 - 2016 the original author or authors.
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
package org.perfcake.reporting.reporters;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ClassifyingReporterTest extends TestSetup {

   @Test
   public void basicClassifyingReporterTest() throws PerfCakeException, InterruptedException {
      final Scenario scenario = ScenarioLoader.load("test-class");
      scenario.init();
      scenario.run();
      scenario.close();

      final ScenarioRetractor scenarioRetractor = new ScenarioRetractor(scenario);
      final DummyDestination dummyDestination = (DummyDestination) scenarioRetractor.getReportManager().getReporters().iterator().next().getDestinations().iterator().next();
      final Measurement measurement = dummyDestination.getLastMeasurement();
      final LongAdder sum = new LongAdder();
      final LongAdder threads = new LongAdder();

      measurement.getAll().forEach((key, value) -> {
         if (key.startsWith("thread_")) {
            sum.add((long) value);
            threads.increment();
         }
      });

      Assert.assertTrue(sum.longValue() > 99_990);
      Assert.assertEquals(threads.longValue(), 10);
   }

}