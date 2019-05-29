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
package org.perfcake.reporting;

import org.perfcake.RunInfo;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.destination.Destination;
import org.perfcake.reporting.reporter.Reporter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Controls the reporting facilities.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ReportManager {

   /**
    * Logger of the class.
    */
   private static final Logger log = LogManager.getLogger(ReportManager.class);

   /**
    * Signaling the time based reporting thread to reset the recorded last reported times and start from scratch.
    */
   private volatile boolean resetLastTimes = false;

   /**
    * Set of reporters registered for reporting.
    */
   private final Set<Reporter> reporters = new CopyOnWriteArraySet<>();

   /**
    * Current run info to control the measurement.
    */
   private RunInfo runInfo;

   /**
    * Thread to assure time based periodical reporting.
    */
   private Thread periodicThread;

   /**
    * Executor to synchronize reporting, for reporting not to need to be thread safe. All reporting is executed out of
    * sender threads, in a separate thread.
    */
   private ThreadPoolExecutor reportingTasks;

   /**
    * We need to be really fast.
    */
   private boolean isTraceEnabled = false;

   /**
    * Creates a new {@link org.perfcake.reporting.MeasurementUnit measurement unit} with a unique iteration number.
    *
    * @return A {@link org.perfcake.reporting.MeasurementUnit measurement unit} unit with a unique iteration number, or null if a measurement is not running or is already finished.
    */
   public MeasurementUnit newMeasurementUnit() {
      if (runInfo.isRunning()) {
         return new MeasurementUnit(runInfo.getNextIteration());
      }

      return null;
   }

   /**
    * Sets {@link org.perfcake.RunInfo} for the current measurement run.
    *
    * @param runInfo
    *       The RunInfo that contains information about the current measurement.
    */
   public void setRunInfo(final RunInfo runInfo) {
      if (log.isDebugEnabled()) {
         log.debug("A new run info set " + runInfo);
      }

      this.runInfo = runInfo;
      for (final Reporter r : reporters) {
         r.setRunInfo(runInfo);
      }
   }

   /**
    * Gets {@link RunInfo} associated with this reporter.
    *
    * @return The {@link RunInfo} associated with this reporter.
    */
   public RunInfo getRunInfo() {
      return runInfo;
   }

   /**
    * Reports a newly measured {@link MeasurementUnit}. Each Measurement Unit must be reported exactly once.
    *
    * @param measurementUnit
    *       A MeasurementUnit to be reported.
    * @throws ReportingException
    *       If reporting could not be done properly.
    */
   public void report(final MeasurementUnit measurementUnit) throws ReportingException {
      if (reportingTasks != null) {
         try {
            reportingTasks.submit(() -> {
               if (isTraceEnabled) {
                  log.trace("Reporting a new measurement unit " + measurementUnit);
               }

               if (runInfo.isStarted()) { // cannot use isRunning while we still want the last iteration to be reported
                  for (final Reporter r : getReporters()) {
                     try {
                        r.report(measurementUnit);
                     } catch (final ReportingException re) {
                        log.error("Error reporting a measurement unit " + measurementUnit, re);
                     }
                  }
               } else {
                  if (log.isDebugEnabled()) {
                     log.debug("Skipping the measurement unit (" + measurementUnit + ") because the ReportManager is not started.");
                  }
               }
            });
         } catch (RejectedExecutionException ree) {
            // Nps, we are likely to be rejecting tasks because we ended the execution either in case of time bounded scenario or because we run out of the shutdown period.
            // We could synchronize all incoming threads on an AtomicInteger inside of executor service. However, this would be a more disruptive way from the performance test point of view.
            if (log.isDebugEnabled()) {
               log.debug("Rejected measurement unit reporting because of the test shutdown in progress.");
            }
         }
      }
   }

   /**
    * Resets reporting to the zero state. It is used after the warm-up period.
    */
   public void reset() {
      isTraceEnabled = log.isTraceEnabled();

      if (log.isDebugEnabled()) {
         log.debug("Resetting reporting.");
      }

      resetReportingTasks();

      runInfo.reset();
      resetLastTimes = true;
      reporters.forEach(org.perfcake.reporting.reporter.Reporter::reset);
   }

   /**
    * Resets the executor of reporting tasks.
    */
   private void resetReportingTasks() {
      final ThreadPoolExecutor oldTasks = reportingTasks;
      reportingTasks = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

      if (oldTasks != null) {
         oldTasks.shutdownNow();
         try {
            oldTasks.awaitTermination(5, TimeUnit.SECONDS);
         } catch (InterruptedException ie) {
            log.info("Could not terminate pre-warmup reporting tasks.");
         }
      }
   }

   /**
    * Registers a new {@link org.perfcake.reporting.reporter.Reporter}.
    *
    * @param reporter
    *       A reporter to be registered.
    */
   public void registerReporter(final Reporter reporter) {
      if (log.isDebugEnabled()) {
         log.debug("Registering a new reporter " + reporter);
      }

      reporter.setReportManager(this);
      reporter.setRunInfo(runInfo);
      reporters.add(reporter);
   }

   /**
    * Removes a registered {@link org.perfcake.reporting.reporter.Reporter}.
    *
    * @param reporter
    *       A reporter to unregistered.
    */
   public void unregisterReporter(final Reporter reporter) {
      if (log.isDebugEnabled()) {
         log.debug("Removing the reporter " + reporter);
      }

      reporter.setReportManager(null);
      reporter.setRunInfo(null);
      reporters.remove(reporter);
   }

   /**
    * Gets an immutable list of current reporters.
    *
    * @return An immutable list of currently registered reporters.
    */
   public Set<Reporter> getReporters() {
      return Collections.unmodifiableSet(reporters);
   }

   /**
    * Starts the reporting facility.
    */
   public void start() {
      if (log.isDebugEnabled()) {
         log.debug("Starting reporting and all reporters.");
      }

      resetReportingTasks();

      runInfo.start(); // runInfo must be started first, otherwise the time monitoring thread in AbstractReporter dies immediately

      reporters.forEach(org.perfcake.reporting.reporter.Reporter::start);

      periodicThread = new Thread(() -> {
         long now;
         Long lastTime;
         Destination d;
         Map<Reporter, Map<Destination, Long>> reportLastTimes = new HashMap<>();
         Map<Destination, Long> lastTimes;

         try {
            while ((runInfo.isRunning() && !periodicThread.isInterrupted())) {

               if (resetLastTimes) {
                  reportLastTimes = new HashMap<>();
                  resetLastTimes = false;
               }

               now = System.currentTimeMillis();

               for (final Reporter r : reporters) {
                  lastTimes = reportLastTimes.get(r);

                  if (lastTimes == null) {
                     lastTimes = new HashMap<>();
                     reportLastTimes.put(r, lastTimes);
                  }

                  for (final BoundPeriod<Destination> p : r.getReportingPeriods()) {
                     d = p.getBinding();
                     lastTime = lastTimes.get(d);

                     if (lastTime == null) {
                        lastTime = now;
                        lastTimes.put(d, lastTime);
                     }

                     if (p.getPeriodType() == PeriodType.TIME && lastTime + p.getPeriod() < now && runInfo.getIteration() >= 0) {
                        lastTimes.put(d, now);
                        try {
                           r.publishResult(PeriodType.TIME, d);
                        } catch (final ReportingException e) {
                           log.warn("Unable to publish result: ", e);
                        }
                     }
                  }
               }

               Thread.sleep(500);
            }
         } catch (final InterruptedException e) {
            // this means our job is done
         }
         if (log.isDebugEnabled()) {
            log.debug("Gratefully terminating the periodic reporting thread.");
         }
      });
      periodicThread.setDaemon(true); // allow the thread to die with JVM termination and do not block it
      periodicThread.start();
   }

   /**
    * Gets the current {@link Thread} used for periodic ticking.
    *
    * @return The current {@link Thread} used for periodic ticking.
    */
   Thread getPeriodicThread() {
      return periodicThread;
   }

   /**
    * Makes sure 100% is reported for time based destinations after the end of the test.
    */
   private void reportFinalTimeResults() {
      log.info("Checking whether there are more results to be reported...");
      for (final Reporter r : reporters) {
         for (final BoundPeriod<Destination> bp : r.getReportingPeriods()) {
            try {
               if (bp.getPeriodType() == PeriodType.TIME) {
                  r.publishResult(bp.getPeriodType(), bp.getBinding());
               }
            } catch (final ReportingException e) {
               log.error(String.format("Could not report final result for reporter %s and destination %s.", r.toString(), bp.getBinding().toString()), e);
            }
         }
      }
      if (log.isDebugEnabled()) {
         log.debug("All results reported.");
      }
   }

   /**
    * Stops the reporting facility.
    * It must not be called sooner than all SenderTasks have been completed.
    */
   public void stop() {
      if (log.isDebugEnabled()) {
         log.debug("Stopping reporting and all reporters.");
      }

      // in case of time bound run, we want to terminate measurement immediately
      if (runInfo.getDuration().getPeriodType() == PeriodType.TIME) {
         runInfo.stop();
         waitForReportingTasks();
      } else { // in case of iteration bound run, we want to wait for senders to complete their execution
         waitForReportingTasks();
         runInfo.stop();
      }

      reportFinalTimeResults();

      reporters.forEach(org.perfcake.reporting.reporter.Reporter::stop);

      if (periodicThread != null) {
         periodicThread.interrupt();
      }
      periodicThread = null;
   }

   /**
    * Gets the number of reporting tasks in the queue awaiting to be processed.
    *
    * @return The number of reporting tasks in the queue awaiting to be processed.
    */
   private long getTasksInQueue() {
      return reportingTasks.getTaskCount() - reportingTasks.getCompletedTaskCount();
   }

   /**
    * Shutdowns reporting task thread and waits for the reporting tasks to be finished.
    */
   private void waitForReportingTasks() {
      // in case of time bound execution, we do not want to see any more results
      if (runInfo.getDuration().getPeriodType() == PeriodType.TIME) {
         reportingTasks.shutdown();
         adaptiveTermination();
         reportingTasks = null;
      } else {
         adaptiveTermination();

         if (periodicThread != null && periodicThread.isAlive()) { // give destinations a chance to complete
            try {
               Thread.sleep(1000);
            } catch (InterruptedException ie) {
               // no problem
            }
         }

         reportingTasks.shutdown();

         reportingTasks = null;
      }
   }

   /**
    * Waits for reporting tasks to be finished.
    */
   private void adaptiveTermination() {
      long lastTasks = 0;
      long tasks = getTasksInQueue();

      while (tasks > 0 && tasks != lastTasks) {
         lastTasks = tasks;

         try {
            reportingTasks.awaitTermination(1, TimeUnit.SECONDS);
         } catch (InterruptedException ie) {
            // no problem
         }

         tasks = getTasksInQueue();
      }
   }
}
