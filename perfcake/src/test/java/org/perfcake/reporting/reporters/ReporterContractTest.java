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
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.destinations.DummyDestination;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thoroughly verifies the proclaimed contract of {@link org.perfcake.reporting.reporters.Reporter}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "integration" })
public class ReporterContractTest {

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

   @Test
   public void reportersRegistrationTest() {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));
      final ResponseTimeStatsReporter r1 = new ResponseTimeStatsReporter();
      final ResponseTimeStatsReporter r2 = new ResponseTimeStatsReporter();

      rm.registerReporter(r1);
      rm.setRunInfo(ri);
      rm.registerReporter(r2);

      Assert.assertEquals(r1.runInfo, ri); // make sure the change was propagated
      Assert.assertEquals(r2.runInfo, ri); // make sure the change was propagated

      rm.unregisterReporter(r2);
      rm.registerReporter(r1); // this should be ignored
      Assert.assertEquals(rm.getReporters().size(), 1);
      Assert.assertFalse(rm.getReporters().contains(r2));

      rm.registerReporter(r2);
      Assert.assertTrue(rm.getReporters().contains(r1));
      Assert.assertTrue(rm.getReporters().contains(r2));
   }

   @Test
   public void reportManagerLifecycleTest() throws ReportingException {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));
      final DummyReporter dr = new DummyReporter();

      rm.setRunInfo(ri);
      rm.registerReporter(dr);

      final MeasurementUnit mu = rm.newMeasurementUnit();
      Assert.assertNull(mu, "No measurement unit should have been returned as the measurement was not started yet.");

      rm.report(mu);
      Assert.assertNull(dr.getLastMethod(), "No value should have been reported as reporting was not started.");

   }

   @Test
   public void measurementUnitTest() throws ReportingException, InterruptedException {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));

      rm.setRunInfo(ri);
      rm.start();

      MeasurementUnit mu = null;
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

   @Test
   public void runInfoTest() throws ReportingException, InterruptedException {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));
      final DummyReporter dr = new DummyReporter();

      rm.setRunInfo(ri);
      rm.registerReporter(dr);

      Assert.assertEquals(ri.getPercentage(), 0d); // we did not run
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getStartTime(), -1);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getRunTime(), 0);

      MeasurementUnit mu = rm.newMeasurementUnit();
      Assert.assertNull(mu);

      rm.start(); // this logs a warnings because we did not add any destinations
      mu = rm.newMeasurementUnit();

      mu.startMeasure();
      Thread.sleep(500);
      mu.stopMeasure();

      rm.report(mu);

      int tries = 0;
      while (dr.getLastMethod() == null && tries++ < 10) {
         Thread.sleep(10);
      }

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

   @Test
   public void reportingPeriodTest() throws ReportingException, InterruptedException {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));
      final ResponseTimeStatsReporter r1 = new ResponseTimeStatsReporter();
      final ResponseTimeStatsReporter r2 = new ResponseTimeStatsReporter();
      final DummyDestination d1 = new DummyDestination();
      final DummyDestination d2 = new DummyDestination();
      final DummyDestination d3 = new DummyDestination();

      rm.setRunInfo(ri);
      rm.registerReporter(r1);
      rm.registerReporter(r2);

      r1.registerDestination(d1, new Period(PeriodType.ITERATION, 100));
      r1.registerDestination(d2, new Period(PeriodType.PERCENTAGE, 8));
      r1.registerDestination(d1, new Period(PeriodType.TIME, 2000));

      final Set<BoundPeriod<Destination>> bp = new HashSet<>();
      bp.add(new BoundPeriod<>(PeriodType.ITERATION, 100, d1));
      bp.add(new BoundPeriod<>(PeriodType.PERCENTAGE, 8, d2));
      bp.add(new BoundPeriod<>(PeriodType.TIME, 2000, d1));
      Assert.assertTrue(r1.getReportingPeriods().containsAll(bp));

      r2.registerDestination(d3, new Period(PeriodType.TIME, 1500));
      r2.setWindowSize(4);

      rm.start();

      final AtomicInteger crc = new AtomicInteger(0);

      d1.setReportAssert(new DummyDestination.ReportAssert() {

         private boolean first = true;

         @SuppressWarnings("unchecked")
         @Override
         public void report(final Measurement m) {
            if (first) {
               Assert.assertEquals(m.getIteration(), 0L);
               Assert.assertEquals(((Quantity<Number>) m.get()).getNumber().longValue(), 10);
               Assert.assertEquals(m.get("avg"), 0d);
               Assert.assertEquals(m.get("it"), "1");

               first = false;
            } else {
               Assert.assertEquals(m.getIteration(), 99L);
               Assert.assertEquals(((Quantity<Number>) m.get()).getNumber().longValue(), 10);
               Assert.assertEquals(m.get("avg"), 49.5d);
               Assert.assertEquals(m.get("it"), "100");
               crc.incrementAndGet(); // this block will be executed twice, first for iteration, second for time
            }
         }
      });

      d2.setReportAssert(new DummyDestination.ReportAssert() {

         private int run = 0;

         @SuppressWarnings("unchecked")
         @Override
         public void report(final Measurement m) {
            if (run == 0) {
               Assert.assertEquals(m.getPercentage(), 0);
               Assert.assertEquals(((Quantity<Number>) m.get()).getNumber().longValue(), 10);
               Assert.assertEquals(m.get("avg"), 0d);

               run = 1;
            } else if (run == 1) {
               Assert.assertEquals(m.getPercentage(), 8);
               Assert.assertEquals(((Quantity<Number>) m.get()).getNumber().longValue(), 10);
               Assert.assertEquals(m.get("avg"), 39.5d);
               crc.incrementAndGet();
               run = 2;
            } else {
               Assert.assertEquals(m.getPercentage(), 10);
               Assert.assertEquals(((Quantity<Number>) m.get()).getNumber().longValue(), 10);
               Assert.assertEquals(m.get("avg"), 49.5d);
               crc.incrementAndGet();
               run = 3;
            }
         }
      });

      d3.setReportAssert(new DummyDestination.ReportAssert() {

         @Override
         public void report(final Measurement m) {
            Assert.assertEquals(m.get("avg"), 97.5d);
         }
      });

      MeasurementUnit mu = null;
      for (int i = 1; i <= 100; i++) {
         mu = rm.newMeasurementUnit();
         mu.startMeasure();
         Thread.sleep(10);
         mu.stopMeasure();
         mu.appendResult("avg", (double) i - 1); // AvgAccumulator should be used
         mu.appendResult("it", String.valueOf(i)); // LastValueAccumulator should be used
         // 15 is the tolerance according to Puškvorec's constant
         Assert.assertTrue(mu.getTotalTime() < 15.0 && mu.getTotalTime() >= 10.0, "Measurement run for 10ms, so the value should not be much different. Current value was: " + mu.getTotalTime());
         rm.report(mu);
      }
      Assert.assertEquals(mu.getIteration(), 99);
      Assert.assertTrue(mu.getTotalTime() < 13.0 && mu.getTotalTime() >= 10.0);
      Assert.assertTrue(mu.getTotalTime() < 13.0 && mu.getTotalTime() >= 10.0);
      Assert.assertEquals(d1.getLastType(), PeriodType.ITERATION);
      Assert.assertEquals(d2.getLastType(), PeriodType.PERCENTAGE);

      Thread.sleep(2500);

      int tries = 0;
      while (d1.getLastType() == null && tries++ < 100) {
         Thread.sleep(100);
      }

      Assert.assertEquals(d1.getLastType(), PeriodType.TIME);
      d3.setReportAssert(null);

      rm.stop();

      Assert.assertEquals(crc.get(), 4);
   }

   @Test
   public void reportManagerResetTest() throws ReportingException, InterruptedException {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));
      final ResponseTimeStatsReporter r1 = new ResponseTimeStatsReporter();
      final ResponseTimeStatsReporter r2 = new ResponseTimeStatsReporter();
      final DummyReporter dr = new DummyReporter();
      final DummyDestination d1 = new DummyDestination();
      final DummyDestination d2 = new DummyDestination();
      final DummyDestination d3 = new DummyDestination();

      rm.setRunInfo(ri);
      rm.registerReporter(r1);
      rm.registerReporter(r2);
      rm.registerReporter(dr);

      r1.registerDestination(d1, new Period(PeriodType.ITERATION, 100));
      r1.registerDestination(d2, new Period(PeriodType.PERCENTAGE, 8));
      r1.registerDestination(d1, new Period(PeriodType.TIME, 2000));

      r2.registerDestination(d3, new Period(PeriodType.TIME, 1500));
      r2.setWindowSize(4);

      rm.start();
      MeasurementUnit mu;
      for (int i = 1; i <= 10; i++) {
         mu = rm.newMeasurementUnit();
         mu.startMeasure();
         Thread.sleep(10);
         mu.stopMeasure();
         mu.appendResult("avg", (double) i - 1); // AvgAccumulator should be used
         mu.appendResult("it", String.valueOf(i)); // LastValueAccumulator should be used
         rm.report(mu);
      }

      rm.stop();

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

   @Test(timeOut = 10000)
   public void finishReachTest() throws ReportingException, InterruptedException {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));
      final DummyReporter dr = new DummyReporter();
      final DummyDestination d1 = new DummyDestination();

      rm.setRunInfo(ri);
      rm.registerReporter(dr);
      dr.registerDestination(d1, new Period(PeriodType.ITERATION, 100));

      rm.start();
      double lastPercentage = 0;
      long lastIteration = 0;

      MeasurementUnit mu;
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
}
