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
package org.perfcake.message.generator;

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.ReportManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generator that is able to generate maximal load given the number of threads configured..
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class DefaultMessageGenerator extends AbstractMessageGenerator {

   /**
    * Shutdown log message.
    */
   private static final String SHUTDOWN_LOG = "Shutting down execution...";

   /**
    * The generator's logger.
    */
   private static final Logger log = LogManager.getLogger(DefaultMessageGenerator.class);

   /**
    * The period in milliseconds in which the thread queue is filled with new tasks.
    */
   protected long monitoringPeriod = 1000; // default 1s

   /**
    * Gets the shutdown period.
    * During a shutdown, the thread queue is regularly checked for the threads finishing their work.
    * It the same amount of threads keeps running for this period, they are forcefully stopped.
    *
    * @return Current shutdown period in ms.
    */
   public long getShutdownPeriod() {
      return shutdownPeriod;
   }

   /**
    * Sets the shutdown period which tells how frequently we should check for threads finishing their work.
    * During a shutdown, the thread queue is regularly checked for the threads finishing their work.
    * It the same amount of threads keeps running for this period, they are forcefully stopped.
    *
    * @param shutdownPeriod The new shutdown period to be set.
    */
   public void setShutdownPeriod(final long shutdownPeriod) {
      this.shutdownPeriod = shutdownPeriod;
   }

   /**
    * During a shutdown, the thread queue is regularly checked for the threads finishing their work.
    * It the same amount of threads keeps running for this period, they are forcefully stopped.
    * The unit of this value is milliseconds. The default value is 1000ms.
    */
   protected long shutdownPeriod = 1000;

   /**
    * The size of internal queue of prepared sender tasks. The default value is 1000 tasks.
    */
   protected int senderTaskQueueSize = 1000;

   /**
    * Controls the maximal number of threads running in parallel.
    */
   protected Semaphore semaphore;

   @Override
   public void setReportManager(final ReportManager reportManager) {
      super.setReportManager(reportManager);
   }

   /**
    * Assigns nice names to threads that send messages and increases their default priority slightly.
    * All threads are set at daemon by default for PerfCake to be able to finish even if some of then hung up.
    */
   static class DaemonThreadFactory implements ThreadFactory {
      private static final AtomicInteger poolNumber = new AtomicInteger(1);
      private final ThreadGroup group;
      private final AtomicInteger threadNumber = new AtomicInteger(1);
      private final String namePrefix;

      DaemonThreadFactory() {
         SecurityManager s = System.getSecurityManager();
         group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
         namePrefix = "PerfCake-" + poolNumber.getAndIncrement() + "-sender-thread-";
      }

      public Thread newThread(final Runnable r) {
         Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
         t.setDaemon(true);
         t.setPriority(8);
         return t;
      }
   }

   /**
    * Place a new {@link SenderTask} implementing the message sending to an internal thread queue.
    *
    * @return True if and only if the task has been successfully submitted.
    * @throws java.lang.InterruptedException When it was not possible to place another task because the queue was empty
    */
   protected boolean prepareTask() throws InterruptedException {
      if (log.isTraceEnabled()) {
         log.trace("Preparing a sender task");
      }

      if (semaphore.tryAcquire(monitoringPeriod, TimeUnit.MILLISECONDS)) {
         executorService.submit(newSenderTask(semaphore));
         return true;
      }

      return false;
   }

   /**
    * Adaptive termination of sender tasks. Waits for tasks to be finished. While they are some tasks remaining and some of them get terminated, keep waiting.
    *
    * @throws InterruptedException
    */
   private void adaptiveTermination() throws InterruptedException {
      executorService.shutdown();
      int active = executorService.getActiveCount(), lastActive = 0;

      while (active > 0 && lastActive != active) { // make sure the threads are finishing
         lastActive = active;
         executorService.awaitTermination(shutdownPeriod, TimeUnit.MILLISECONDS);
         active = executorService.getActiveCount();

         if (log.isDebugEnabled()) {
            log.debug(String.format("Adaptive test execution termination in progress. Tasks finished in last round: %d", lastActive - active));
         }
      }

      if (active > 0) {
         log.warn("Cannot terminate all sender tasks. Remaining tasks active: " + active);
      }
   }

   /**
    * Takes care of gentle shutdown of the generator based on the period type.
    *
    * @throws java.lang.InterruptedException When waiting for the termination was interrupted.
    */
   protected void shutdown() throws InterruptedException {
      if (runInfo.getDuration().getPeriodType() == PeriodType.ITERATION) { // in case of iterations, we wait for the tasks to be finished first
         log.info(SHUTDOWN_LOG);
         adaptiveTermination();
         setStopTime();
      } else { // in case of time, we must stop measurement first
         setStopTime();
         log.info(SHUTDOWN_LOG);
         adaptiveTermination();
      }

      executorService.shutdownNow();
   }

   @Override
   public void generate() throws Exception {
      log.info("Starting to generate...");
      semaphore = new Semaphore(senderTaskQueueSize);
      executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(getThreads(), new DaemonThreadFactory());
      runInfo.setThreads(getThreads());
      setStartTime();

      if (runInfo.getDuration().getPeriodType() == PeriodType.ITERATION) {
         long i = 0;
         final long max = runInfo.getDuration().getPeriod();
         while (i < max) {
            if (prepareTask()) {
               i = i + 1; // long does not work with i++
            }
         }
      } else {
         while (runInfo.isRunning()) {
            prepareTask();
         }
      }

      log.info("Reached test end.");
      shutdown();
   }

   /**
    * Used to read the value of monitoringPeriod.
    *
    * @return The monitoringPeriod.
    */
   public long getMonitoringPeriod() {
      return monitoringPeriod;
   }

   /**
    * Sets the value of monitoringPeriod.
    *
    * @param monitoringPeriod The monitoringPeriod to set.
    * @return this
    */
   public DefaultMessageGenerator setMonitoringPeriod(final long monitoringPeriod) {
      this.monitoringPeriod = monitoringPeriod;
      return this;
   }

   /**
    * Used to read the amount of time (in seconds) for which the generator will generate the measured load.
    *
    * @return The duration.
    */
   public long getDuration() {
      return runInfo.getDuration().getPeriod();
   }

   /**
    * Used to read the size of the internal thread queue.
    *
    * @return The thread queue size.
    */
   public int getSenderTaskQueueSize() {
      return senderTaskQueueSize;
   }

   /**
    * Sets the the size of the internal thread queue.
    *
    * @param senderTaskQueueSize The thread queue size.
    * @return this
    */
   public DefaultMessageGenerator setSenderTaskQueueSize(final int senderTaskQueueSize) {
      this.senderTaskQueueSize = senderTaskQueueSize;
      return this;
   }

   @Override
   protected void validateRunInfo() {
      if (runInfo.getDuration().getPeriodType() == PeriodType.PERCENTAGE) {
         throw new IllegalStateException(String.format("%s can only be used with an iteration based run configuration.", this.getClass().getName()));
      }
   }

}
