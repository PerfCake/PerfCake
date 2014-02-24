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

import org.apache.log4j.Logger;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.ReportManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p> Generator that is able to generate maximal load. </p>
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class DefaultMessageGenerator extends AbstractMessageGenerator {

   /**
    * The generator's logger.
    */
   private static final Logger log = Logger.getLogger(DefaultMessageGenerator.class);

   /**
    * The period in milliseconds in which the thread queue is filled with new tasks.
    */
   protected long monitoringPeriod = 1000; // default 1s

   /**
    * The size of internal thread queue.
    */
   protected int threadQueueSize = 1000; // default

   @Override
   public void setReportManager(final ReportManager reportManager) {
      super.setReportManager(reportManager);
   }

   /**
    * Place a specified number of {@link SenderTask} implementing the message sending task to internal thread queue.
    *
    * @param count
    *       The number of {@link SenderTask};
    */
   private void sendPack(final long count) {
      if (log.isDebugEnabled()) {
         log.debug("Submitting " + count + " sender tasks...");
      }
      for (long i = 0; i < count; i++) {
         if (runInfo.isRunning()) { // didn't we run out of iterations or time?
            executorService.submit(newSenderTask());
         }
      }
   }

   /**
    * Adaptive termination of sender tasks. Waits for tasks to be finished. While they are some tasks remaining and some of them get terminated, keep waiting.
    *
    * @throws InterruptedException
    */
   private void adaptiveTermination() throws InterruptedException {
      executorService.shutdown();
      int active = executorService.getActiveCount(), lastActive = 0;

      while (active > 0 && lastActive != active) { // make sure the threads arSubmitinge finishing
         lastActive = active;
         executorService.awaitTermination(monitoringPeriod, TimeUnit.MILLISECONDS);
         active = executorService.getActiveCount();

         if (log.isDebugEnabled()) {
            log.debug(String.format("Adaptive test execution termination in progress. Tasks finished in last round: %d", lastActive - active));
         }
      }
   }

   private void shutdown() throws InterruptedException {
      if (runInfo.getDuration().getPeriodType() == PeriodType.ITERATION) { // in case of iterations, we wait for the tasks to be finished first
         adaptiveTermination();
         setStopTime();
      } else { // in case of time, we must stop measurement first
         setStopTime();
         adaptiveTermination();

      }

      executorService.shutdownNow();
   }

   @Override
   public void generate() throws Exception {
      log.info("Starting to generate...");
      executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
      setStartTime();
      sendPack(threadQueueSize);

      while (runInfo.isRunning()) {
         sendPack(threadQueueSize - executorService.getQueue().size());
         if (runInfo.isRunning()) { // we don't want to waste time here when the time is over, we must stop quickly
            Thread.sleep(monitoringPeriod);
         }
      }

      log.info("Reached test end. Shutting down execution...");
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
    * @param monitoringPeriod
    *       The monitoringPeriod to set.
    */
   public void setMonitoringPeriod(final long monitoringPeriod) {
      this.monitoringPeriod = monitoringPeriod;
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
   public int getThreadQueueSize() {
      return threadQueueSize;
   }

   /**
    * Sets the the size of the internal thread queue.
    *
    * @param threadQueueSize
    *       The thread queue size.
    */
   public void setThreadQueueSize(final int threadQueueSize) {
      this.threadQueueSize = threadQueueSize;
   }

   @Override
   protected void validateRunInfo() {
      if (runInfo.getDuration().getPeriodType() == PeriodType.PERCENTAGE) {
         throw new IllegalStateException(String.format("%s can only be used with an iteration based run configuration.", this.getClass().getName()));
      }
   }

}
