package org.perfcake.nreporting.reporters;

import java.util.HashSet;
import java.util.Set;

import org.perfcake.RunInfo;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.nreporting.MeasurementUnit;
import org.perfcake.nreporting.ReportManager;
import org.perfcake.nreporting.ReportingException;
import org.perfcake.nreporting.destinations.Destination;
import org.perfcake.nreporting.destinations.DummyDestination;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ReporterContractTest {

   @Test
   public void noRunInfoTest() throws ReportingException {
      ResponseTimeReporter r = new ResponseTimeReporter();

      Exception e = null;
      try {
         r.report(null);
      } catch (ReportingException ee) {
         e = ee;
      }
      Assert.assertNotNull(e, "An exception was supposed to be thrown as RunInfo was not set.");
   }

   // TODO split into multiple tests
   @Test
   public void f() throws ReportingException, InterruptedException {
      ReportManager rm = new ReportManager();
      RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));
      ResponseTimeReporter r1 = new ResponseTimeReporter();
      ResponseTimeReporter r2 = new ResponseTimeReporter();
      DummyReporter dr = new DummyReporter();

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
      MeasurementUnit mu = null;
      for (int i = 0; i <= 500; i++) {
         mu = rm.newMeasurementUnit();
      }
      Assert.assertEquals(mu.getIteration(), 500);
      Assert.assertEquals(mu.getLastTime(), -1);
      Assert.assertEquals(mu.getTotalTime(), 0);

      mu.startMeasure();
      Thread.sleep(500);
      mu.stopMeasure();
      Assert.assertTrue(mu.getTotalTime() < 600); // we slept only for 500ms
      Assert.assertTrue(mu.getLastTime() < 600);

      mu.startMeasure();
      Thread.sleep(500);
      mu.stopMeasure();
      Assert.assertTrue(mu.getTotalTime() < 1200); // we slept only for 2x500ms
      Assert.assertTrue(mu.getLastTime() < 600);

      Assert.assertEquals(ri.getPercentage(), 0d); // we did not run
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getStartTime(), -1);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getRunTime(), 0);

      rm.report(mu);
      Assert.assertNull(dr.getLastMethod(), "No value should have been reported as reporting was not started.");

      rm.start(); // this logs 3 warnings because we did not add any destinations
      rm.report(mu);
      long startTime = ri.getStartTime();
      Assert.assertTrue(ri.isRunning());
      Assert.assertEquals(dr.getLastMethod(), "doReport");

      rm.reset();
      Assert.assertEquals(dr.getLastMethod(), "doReset");

      rm.stop();
      Assert.assertFalse(ri.isRunning());

      DummyDestination d1 = new DummyDestination();
      DummyDestination d2 = new DummyDestination();

      r1.registerDestination(d1, new Period(PeriodType.ITERATION, 100));
      r1.registerDestination(d2, new Period(PeriodType.PERCENTAGE, 10));
      r1.registerDestination(d1, new Period(PeriodType.TIME, 2000));

      Set<BoundPeriod<Destination>> bp = new HashSet<>();
      bp.add(new BoundPeriod<Destination>(PeriodType.ITERATION, 100, d1));
      bp.add(new BoundPeriod<Destination>(PeriodType.PERCENTAGE, 10, d2));
      bp.add(new BoundPeriod<Destination>(PeriodType.TIME, 2000, d1));
      Assert.assertTrue(r1.getReportingPeriods().containsAll(bp));

      Assert.assertEquals(ri.getPercentage(), 0d); // we had reset
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertTrue(ri.getEndTime() > ri.getStartTime());
      Assert.assertTrue(ri.getRunTime() == ri.getEndTime() - ri.getStartTime());
      rm.start();

      mu = null;
      for (int i = 0; i <= 100; i++) {
         mu = rm.newMeasurementUnit();
         mu.startMeasure();
         mu.appendResult("avg", (double) i);
         Thread.sleep(10);
         mu.stopMeasure();
         rm.report(mu);
      }
      Assert.assertEquals(mu.getIteration(), 100);
      Assert.assertEquals(mu.getLastTime(), 10);
      Assert.assertEquals(mu.getTotalTime(), 10);
      Assert.assertEquals(d1.getLastType(), PeriodType.ITERATION);
      Assert.assertEquals(d2.getLastType(), PeriodType.PERCENTAGE);

      Thread.sleep(2500);

      Assert.assertEquals(d1.getLastType(), PeriodType.TIME);

      rm.stop();

      // System.out.println(ri);

      // System.out.println(d1.getLastMeasurement());

      // what happens if last iteration is reached
      // verify percentage increase, proper results handling in ResponseTimeReporter (including various types for accumulated values)
      // reset should reset all the parties
      // time based reporting, percentage based reporting, iteration based reporting
   }
}
