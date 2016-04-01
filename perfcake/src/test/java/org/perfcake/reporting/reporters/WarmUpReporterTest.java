/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
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
import org.perfcake.RunInfo;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.DummyDestination;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests {@link org.perfcake.reporting.reporters.WarmUpReporter} implemetation.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Test(groups = { "unit" })
public class WarmUpReporterTest {
   private static final int COUNT = 50;

   @Test
   public void testWarmUp() throws ReportingException, InterruptedException {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, COUNT));
      final ResponseTimeStatsReporter dr = new ResponseTimeStatsReporter();
      final WarmUpReporter wr = new WarmUpReporter();
      wr.setMinimalWarmUpCount(0);
      wr.setMinimalWarmUpDuration(0);

      rm.setRunInfo(ri);
      rm.registerReporter(wr);
      rm.registerReporter(dr);

      final DummyDestination dd = new DummyDestination();
      dr.registerDestination(dd, new Period(PeriodType.ITERATION, 1));

      rm.start();
      MeasurementUnit mu;
      Measurement m;
      boolean warmUpDetected = false;
      for (int i = 0; i < COUNT; i++) {
         mu = rm.newMeasurementUnit();
         mu.startMeasure();
         Thread.sleep(100);
         mu.stopMeasure();
         rm.report(mu);
         Thread.sleep(100);
         m = dd.getLastMeasurement();
         Assert.assertNotNull(m);
         if (!warmUpDetected && !((Boolean) m.get(PerfCakeConst.WARM_UP_TAG))) {
            warmUpDetected = true;
            i = -1;
            continue;
         }
         Assert.assertEquals(m.getIteration(), i);
      }
      rm.stop();
      Assert.assertTrue(warmUpDetected);
   }
}
