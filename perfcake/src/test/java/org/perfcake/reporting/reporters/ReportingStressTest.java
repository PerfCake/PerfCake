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

import org.perfcake.RunInfo;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.DummyDestination;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Verify that reporting can handle multiple threads.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ReportingStressTest {

   @Test(groups = { "stress" })
   public void threadSafeReportingTest() throws InterruptedException {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.TIME, 10000));
      final ResponseTimeStatsReporter r1 = new ResponseTimeStatsReporter();
      final ResponseTimeStatsReporter r2 = new ResponseTimeStatsReporter();
      final ResponseTimeStatsReporter r3 = new ResponseTimeStatsReporter();
      final DummyDestination d11 = new DummyDestination();
      final DummyDestination d12 = new DummyDestination();
      final DummyDestination d13 = new DummyDestination();
      final DummyDestination d21 = new DummyDestination();
      final DummyDestination d22 = new DummyDestination();
      final DummyDestination d23 = new DummyDestination();
      final DummyDestination d31 = new DummyDestination();
      final DummyDestination d32 = new DummyDestination();
      final DummyDestination d33 = new DummyDestination();

      rm.setRunInfo(ri);
      rm.registerReporter(r1);
      rm.registerReporter(r3);
      rm.registerReporter(r2);

      r1.registerDestination(d11, new Period(PeriodType.ITERATION, 1000));
      r1.registerDestination(d12, new Period(PeriodType.TIME, 6000));
      r1.registerDestination(d13, new Period(PeriodType.PERCENTAGE, 60));

      r2.registerDestination(d21, new Period(PeriodType.ITERATION, 2000));
      r2.registerDestination(d22, new Period(PeriodType.TIME, 7000));
      r2.registerDestination(d23, new Period(PeriodType.PERCENTAGE, 80));

      r3.registerDestination(d31, new Period(PeriodType.ITERATION, 2500));
      r3.registerDestination(d32, new Period(PeriodType.TIME, 8000));
      r3.registerDestination(d33, new Period(PeriodType.PERCENTAGE, 90));

      final List<Thread> threads = new ArrayList<>();

      for (int i = 0; i < 100; i++) {
         threads.add(new Thread(new Runnable() {

            @Override
            public void run() {
               final Random r = new Random();
               MeasurementUnit mu;
               try {
                  while (ri.isRunning()) {
                     mu = rm.newMeasurementUnit();
                     mu.startMeasure();
                     Thread.sleep(r.nextInt(5) + 1);
                     mu.stopMeasure();
                     rm.report(mu);
                  }
               } catch (final InterruptedException e) {
               } catch (final ReportingException e) {
                  Assert.fail("No exception should have been thrown: ", e);
               }
            }

         }));
      }

      rm.start();

      for (final Thread t : threads) {
         t.start();
      }

      for (final Thread t : threads) {
         t.join();
      }

      Assert.assertEquals(safeLastType(d11), PeriodType.ITERATION);
      Assert.assertEquals(safeLastType(d12), PeriodType.TIME);
      Assert.assertEquals(safeLastType(d13), PeriodType.PERCENTAGE);
      Assert.assertEquals(safeLastType(d21), PeriodType.ITERATION);
      Assert.assertEquals(safeLastType(d22), PeriodType.TIME);
      Assert.assertEquals(safeLastType(d23), PeriodType.PERCENTAGE);
      Assert.assertEquals(safeLastType(d31), PeriodType.ITERATION);
      Assert.assertEquals(safeLastType(d32), PeriodType.TIME);
      Assert.assertEquals(safeLastType(d33), PeriodType.PERCENTAGE);

      rm.stop();
   }

   private PeriodType safeLastType(final DummyDestination dd) throws InterruptedException {
      int tries = 0;
      while (dd.getLastType() == null && tries++ < 10) {
         Thread.sleep(100);
      }
      return dd.getLastType();
   }
}
