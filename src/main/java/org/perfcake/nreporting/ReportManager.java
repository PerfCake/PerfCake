package org.perfcake.nreporting;

import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hornetq.utils.ConcurrentHashSet;
import org.perfcake.RunInfo;
import org.perfcake.nreporting.reporters.Reporter;

public class ReportManager {
   private static final Logger log = Logger.getLogger(ReportManager.class);

   private final Set<Reporter> reporters = new ConcurrentHashSet<>();
   private RunInfo runInfo;

   public MeasurementUnit newMeasurementUnit() {
      if (!runInfo.isRunning()) {
         return null;
      }

      return new MeasurementUnit(runInfo.getNextIteration());
   }

   public void setRunInfo(final RunInfo runInfo) {
      this.runInfo = runInfo;
      for (final Reporter r : reporters) {
         r.setRunInfo(runInfo);
      }
   }

   public void report(final MeasurementUnit mu) throws ReportingException {
      if (runInfo.isStarted()) { // cannot use isRunning while we still want the last iteration to be reported
         for (final Reporter r : reporters) {
            r.report(mu);
         }
      } else {
         log.debug("Skipping measurement unit because the ReportManager has not been started yet.");
      }
   }

   public void reset() {
      runInfo.reset();
      for (final Reporter r : reporters) {
         r.reset();
      }
   }

   public void registerReporter(final Reporter reporter) {
      reporter.setRunInfo(runInfo);
      reporters.add(reporter);
   }

   public void unregisterReporter(final Reporter reporter) {
      reporters.remove(reporter);
   }

   public Set<Reporter> getReporters() {
      return Collections.unmodifiableSet(reporters);
   }

   public void start() {
      runInfo.start(); // runInfo must be started first, otherwise the time monitoring thread in AbstractReporter dies immediately

      for (final Reporter r : reporters) {
         r.start();
      }
   }

   public void stop() {
      runInfo.stop();

      for (final Reporter r : reporters) {
         r.stop();
      }
   }

}
