package org.perfcake.nreporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.perfcake.RunInfo;
import org.perfcake.nreporting.reporters.Reporter;

public class ReportManager {
   private static final Logger log = Logger.getLogger(ReportManager.class);

   private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
   private final List<Reporter> reporters = new ArrayList<>();
   private boolean started = false;
   private RunInfo runInfo;

   public MeasurementUnit newMeasurementUnit() {
      return new MeasurementUnit(runInfo.getNextIteration());
   }

   public void setRunInfo(final RunInfo runInfo) {
      this.runInfo = runInfo;
      for (Reporter r : reporters) {
         rwLock.writeLock().lock();
         r.setRunInfo(runInfo);
         rwLock.writeLock().unlock();
      }
   }

   public void report(final MeasurementUnit mu) throws ReportingException {
      if (started) {
         rwLock.readLock().lock();
         for (Reporter r : reporters) {
            r.report(mu);
         }
         rwLock.readLock().unlock();
      } else {
         log.debug("Skipping measurement unit because the ReportManager has not been started yet.");
      }
   }

   public void reset() {
      runInfo.reset();
      for (Reporter r : reporters) {
         rwLock.writeLock().lock();
         r.reset();
         rwLock.writeLock().unlock();
      }
   }

   public void registerReporter(final Reporter reporter) {
      rwLock.writeLock().lock();
      reporter.setRunInfo(runInfo);
      reporters.add(reporter);
      rwLock.writeLock().unlock();
   }

   public void unregisterReporter(final Reporter reporter) {
      rwLock.writeLock().lock();
      reporters.remove(reporter);
      rwLock.writeLock().unlock();
   }

   public List<Reporter> getReporters() {
      return Collections.unmodifiableList(reporters);
   }

   public void start() {
      for (Reporter r : reporters) {
         rwLock.writeLock().lock();
         r.start();
         rwLock.writeLock().unlock();
      }

      started = true;
      runInfo.start();
   }

   public void stop() {
      started = false;
      runInfo.stop();

      for (Reporter r : reporters) {
         rwLock.writeLock().lock();
         r.stop();
         rwLock.writeLock().unlock();
      }
   }

}
