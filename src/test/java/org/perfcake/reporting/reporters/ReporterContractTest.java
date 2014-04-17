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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.perfcake.RunInfo;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.reporting.destinations.DummyDestination.ReportAssert;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ReporterContractTest {

   private final ReportManager rm = new ReportManager();
   private final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));
   private final ResponseTimeStatsReporter r1 = new ResponseTimeStatsReporter();
   /** TODO: use window feature of ResponseTimeStatsReporter once implemented */
   // private final WindowResponseTimeReporter r2 = new WindowResponseTimeReporter();
   private final ResponseTimeStatsReporter r2 = new ResponseTimeStatsReporter();
   private final DummyReporter dr = new DummyReporter();
   private final DummyDestination d1 = new DummyDestination();
   private final DummyDestination d2 = new DummyDestination();
   private final DummyDestination d3 = new DummyDestination();
   private MeasurementUnit mu = null;

   @Test
   public void noRunInfoTest() throws ReportingException {
      final ResponseTimeStatsReporter r = new ResponseTimeStatsReporter();

      Exception e = null;
      try {
         r.report(null);
      } catch (final ReportingException ee) {
         e = ee;
      }
      Assert.assertNotNull(e, "An exception was supposed to be thrown as RunInfo was not set.");
   }

   @Test(priority = 1)
   public void reportManagerTest() throws ReportingException, InterruptedException {
      rm.registerReporter(r1);
      rm.setRunInfo(ri);
      rm.registerReporter(r2);

      Assert.assertEquals(r1.runInfo, ri); // make sure the change was propagated
      Assert.assertEquals(r2.runInfo, ri); // make sure the change was propagated

      rm.unregisterReporter(r2);
      rm.registerReporter(r1); // this should be ignored
      Assert.assertEquals(rm.getReporters().size(), 1);

      rm.registerReporter(r2);
      Assert.assertTrue(rm.getReporters().contains(r1));
      Assert.assertTrue(rm.getReporters().contains(r2));

      rm.registerReporter(dr);
      mu = rm.newMeasurementUnit();

      rm.report(mu);
      Assert.assertNull(dr.getLastMethod(), "No value should have been reported as reporting was not started.");
   }

   @Test(priority = 2)
   public void measurementUnitTest() throws ReportingException, InterruptedException {
      rm.start();
      mu = null;
      for (int i = 1; i <= 500; i++) {
         mu = rm.newMeasurementUnit();
      }
      rm.stop();
      rm.reset();
      Assert.assertEquals(mu.getIteration(), 499);
      Assert.assertEquals(mu.getLastTime(), -1.0);
      Assert.assertEquals(mu.getTotalTime(), 0.0);

      mu.startMeasure();
      Thread.sleep(500);
      mu.stopMeasure();
      Assert.assertTrue(mu.getTotalTime() < 600.0); // we slept only for 500ms
      Assert.assertTrue(mu.getLastTime() < 600.0);

      mu.startMeasure();
      Thread.sleep(500);
      mu.stopMeasure();
      Assert.assertTrue(mu.getTotalTime() < 1200.0); // we slept only for 2x500ms
      Assert.assertTrue(mu.getLastTime() < 600.0);
   }

   @Test(priority = 3)
   public void runInfoTest() throws ReportingException, InterruptedException {
      Assert.assertEquals(ri.getPercentage(), 0d); // we did not run
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getStartTime(), -1);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getRunTime(), 0);

      mu = rm.newMeasurementUnit();
      Assert.assertNull(mu);

      rm.start(); // this logs 3 warnings because we did not add any destinations
      mu = rm.newMeasurementUnit();

      mu.startMeasure();
      Thread.sleep(500);
      mu.stopMeasure();

      rm.report(mu);
      Assert.assertTrue(ri.isRunning());
      Assert.assertEquals(dr.getLastMethod(), "doReport");

      rm.reset();
      Assert.assertEquals((long) dr.getMaxIteration(), 0l);
      Assert.assertEquals(dr.getLastMethod(), "doReset");

      Thread.sleep(10);

      rm.stop();
      Assert.assertFalse(ri.isRunning());

      Assert.assertEquals(ri.getPercentage(), 0d); // we had reset
      Assert.assertFalse(ri.isRunning());
      Assert.assertTrue(ri.getEndTime() > ri.getStartTime());
      Assert.assertTrue(ri.getRunTime() == ri.getEndTime() - ri.getStartTime());
   }

   @Test(priority = 4)
   public void reportingPeriodTest() throws ReportingException, InterruptedException {
      r1.registerDestination(d1, new Period(PeriodType.ITERATION, 100));
      r1.registerDestination(d2, new Period(PeriodType.PERCENTAGE, 8));
      r1.registerDestination(d1, new Period(PeriodType.TIME, 2000));

      final Set<BoundPeriod<Destination>> bp = new HashSet<>();
      bp.add(new BoundPeriod<Destination>(PeriodType.ITERATION, 100, d1));
      bp.add(new BoundPeriod<Destination>(PeriodType.PERCENTAGE, 8, d2));
      bp.add(new BoundPeriod<Destination>(PeriodType.TIME, 2000, d1));
      Assert.assertTrue(r1.getReportingPeriods().containsAll(bp));

      r2.registerDestination(d3, new Period(PeriodType.TIME, 1500));

      /** TODO: use window feature of ResponseTimeStatsReporter once implemented */
      // r2.setWindowSize(4);

      rm.start();

      final AtomicInteger crc = new AtomicInteger(0);

      d1.setReportAssert(new ReportAssert() {

         private boolean first = true;

         @Override
         public void report(final Measurement m) {
            if (first) {
               Assert.assertEquals(m.getIteration(), 0L);
               Assert.assertEquals(((Double) m.get()).longValue(), 10);
               Assert.assertEquals(m.get("avg"), 0d);
               Assert.assertEquals(m.get("it"), "1");

               first = false;
            } else {
               Assert.assertEquals(m.getIteration(), 99L);
               Assert.assertEquals(((Double) m.get()).longValue(), 10);
               Assert.assertEquals(m.get("avg"), 49.5d);
               Assert.assertEquals(m.get("it"), "100");
               crc.incrementAndGet(); // this block will be executed twice, first for iteration, second for time
            }
         }
      });

      d2.setReportAssert(new ReportAssert() {

         private int run = 0;

         @Override
         public void report(final Measurement m) {
            if (run == 0) {
               Assert.assertEquals(m.getPercentage(), 0);
               Assert.assertEquals(((Double) m.get()).longValue(), 10);
               Assert.assertEquals(m.get("avg"), 0d);

               run = 1;
            } else if (run == 1) {
               Assert.assertEquals(m.getPercentage(), 8);
               Assert.assertEquals(((Double) m.get()).longValue(), 10);
               Assert.assertEquals(m.get("avg"), 39.5d);
               crc.incrementAndGet();
               run = 2;
            } else {
               Assert.assertEquals(m.getPercentage(), 10);
               Assert.assertEquals(((Double) m.get()).longValue(), 10);
               Assert.assertEquals(m.get("avg"), 49.5d);
               crc.incrementAndGet();
               run = 3;
            }
         }
      });

      d3.setReportAssert(new ReportAssert() {

         @Override
         public void report(final Measurement m) {
            Assert.assertEquals(m.get("avg"), 49.5d);
         }
      });

      mu = null;
      for (int i = 1; i <= 100; i++) {
         mu = rm.newMeasurementUnit();
         mu.startMeasure();
         Thread.sleep(10);
         mu.stopMeasure();
         mu.appendResult("avg", (double) i - 1); // AvgAccumulator should be used
         mu.appendResult("it", String.valueOf(i)); // LastValueAccumulator should be used
         Assert.assertEquals(((Double) mu.getTotalTime()).longValue(), 10, "Measurement runs for 10ms, so the value should not be much different.");
         rm.report(mu);
      }
      Assert.assertEquals(mu.getIteration(), 99);
      Assert.assertEquals(((Double) mu.getLastTime()).longValue(), 10);
      Assert.assertEquals(((Double) mu.getTotalTime()).longValue(), 10);
      Assert.assertEquals(d1.getLastType(), PeriodType.ITERATION);
      Assert.assertEquals(d2.getLastType(), PeriodType.PERCENTAGE);

      Thread.sleep(2500);

      Assert.assertEquals(d1.getLastType(), PeriodType.TIME);
      d3.setReportAssert(null);

      rm.stop();

      Assert.assertEquals(crc.get(), 4);

      d1.setReportAssert(null);
      d2.setReportAssert(null);
   }

   @Test(priority = 5)
   public void reportManagerResetTest() throws ReportingException, InterruptedException {
      rm.reset();
      Assert.assertEquals(ri.getPercentage(), 0d);
      Assert.assertEquals(ri.getIteration(), -1L);
      Assert.assertEquals(ri.getRunTime(), 0L);
      Assert.assertNull(r1.getAccumulatedResult("avg"));
      Assert.assertNull(r1.getAccumulatedResult("it"));
      Assert.assertNull(r1.getAccumulatedResult(Measurement.DEFAULT_RESULT));
      Assert.assertNull(r2.getAccumulatedResult("avg"));
      Assert.assertNull(r2.getAccumulatedResult("it"));
      Assert.assertNull(r2.getAccumulatedResult(Measurement.DEFAULT_RESULT));

      Assert.assertEquals(dr.getLastMethod(), "doReset");
   }

   @Test(priority = 6, timeOut = 10000)
   public void finishReachTest() throws ReportingException, InterruptedException {
      rm.start();
      double lastPercentage = 0;
      long lastIteration = 0;

      while (ri.isRunning()) {
         mu = rm.newMeasurementUnit();
         lastPercentage = ri.getPercentage();
         lastIteration = mu.getIteration();
         rm.report(mu);
      }

      Assert.assertEquals(lastIteration, 999L);
      Assert.assertEquals(lastPercentage, 100d);
      rm.stop();

      Assert.assertEquals(ri.getPercentage(), 100d);
      Assert.assertEquals(ri.getIteration(), 999L);
   }
   // what happens if last iteration is reached
}
