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
package org.perfcake.reporting.reporters;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class MessageSizeReportTest extends TestSetup {

   @Test
   public void sizeReportingTest() throws PerfCakeException {
      final Scenario scenario = ScenarioLoader.load("test-size");

      ScenarioRetractor sr = new ScenarioRetractor(scenario);
      sr.getReportManager();

      scenario.init();
      scenario.run();
      scenario.close();

      final DummyDestination[] dd = new DummyDestination[1];

      sr.getReportManager().getReporters().iterator().next().getDestinations().forEach(destination -> {
         if (destination instanceof DummyDestination) {
            dd[0] = (DummyDestination) destination;
         }
      });

      Assert.assertNotNull(dd[0]);

      @SuppressWarnings("unchecked")
      final Quantity<Long> responseSize = (Quantity<Long>) dd[0].getLastMeasurement().get(PerfCakeConst.RESPONSE_SIZE_TAG);

      @SuppressWarnings("unchecked")
      final Quantity<Long> requestSize = (Quantity<Long>) dd[0].getLastMeasurement().get(PerfCakeConst.REQUEST_SIZE_TAG);

      Assert.assertEquals((long) responseSize.getNumber(), 256000L);
      Assert.assertEquals((long) requestSize.getNumber(), 256000L);
   }
}