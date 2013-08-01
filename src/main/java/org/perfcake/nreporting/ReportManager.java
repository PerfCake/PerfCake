package org.perfcake.nreporting;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.perfcake.RunInfo;
import org.perfcake.nreporting.reporters.Reporter;

public class ReportManager {
   private static final Logger log = Logger.getLogger(ReportManager.class);

   private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
   private final Set<Reporter> reporters = new HashSet<>();
   private RunInfo runInfo;

   public MeasurementUnit newMeasurementUnit() {
      return new MeasurementUnit(runInfo.getNextIteration());
   }

   public void setRunInfo(final RunInfo runInfo) {
      this.runInfo = runInfo;
      rwLock.writeLock().lock();
      for (Reporter r : reporters) {
         r.setRunInfo(runInfo);
      }
      rwLock.writeLock().unlock();
   }

   public void report(final MeasurementUnit mu) throws ReportingException {
      if (runInfo.isRunning()) {
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
      runInfo.start();
      rwLock.writeLock().lock();
      for (Reporter r : reporters) {
         r.reset();
      }
      rwLock.writeLock().unlock();
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

   public Set<Reporter> getReporters() {
      return Collections.unmodifiableSet(reporters);
   }

   public void start() {
      runInfo.start(); // runInfo must be started first, otherwise the time monitoring thread in AbstractReporter dies immediately

      rwLock.writeLock().lock();
      for (Reporter r : reporters) {
         r.start();
      }
      rwLock.writeLock().unlock();
   }

   public void stop() {
      runInfo.stop();

      rwLock.writeLock().lock();
      for (Reporter r : reporters) {
         r.stop();
      }
      rwLock.writeLock().unlock();
   }

}
