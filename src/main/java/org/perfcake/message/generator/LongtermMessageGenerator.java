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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.ReportManager;

/**
 * <p>
 * Time driven generator - generates the load for a specified amount of time. The actual duration is specified by the value of {@link #duration} with the default value of 60s.
 * </p>
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class LongtermMessageGenerator extends AbstractMessageGenerator {

   /**
    * The generator's logger.
    */
   private static final Logger log = Logger.getLogger(LongtermMessageGenerator.class);

   /**
    * TODO: add javadoc comment (internal field)
    */
   protected long monitoringPeriod = 1000; // default 1s

   /**
    * The size of internal thread queue.
    */
   protected int threadQueueSize = 1000; // default

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.generator.AbstractMessageGenerator#setReportManager(org.perfcake.reporting.ReportManager)
    */
   @Override
   public void setReportManager(final ReportManager reportManager) {
      super.setReportManager(reportManager);
   }

   /**
    * Place a specified number of {@link SenderTask} implementing the message sending task to internal thread queue.
    * 
    * @param count
    *           The number of {@link SenderTask};
    */
   private void sendPack(final long count) {
      for (long i = 0; i < count; i++) {
         executorService.submit(new SenderTask(reportManager, messageSenderManager, messageStore, isMessageNumberingEnabled(), isMeasuring));
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.generator.AbstractMessageGenerator#generate()
    */
   @Override
   public void generate() throws Exception {

      isMeasuring = !warmUpEnabled;
      setStartTime();

      if (log.isInfoEnabled()) {
         log.info("Preparing senders");
      }
      executorService = Executors.newFixedThreadPool(threads);
      sendPack(threadQueueSize);

      if (warmUpEnabled && log.isInfoEnabled()) {
         log.info("Warming server up (for at least " + minimalWarmUpDuration + " ms and " + minimalWarmUpCount + " iterations" + ")");
      }
      float lastSpeed = Float.MIN_VALUE; // smallest positive nonzero value
      boolean terminated = false;
      boolean expired = true;
      long lastValue = 0;
      long runTime;
      if (isMeasuring) {
         runTime = getDurationInMillis();
      } else {
         runTime = 0;
      }

      final long duration = runInfo.getDuration().getPeriod();

      while (!(expired = (runTime > duration * 1000)) || !terminated) {
         if (log.isDebugEnabled()) {
            log.debug("Run time: " + runTime + "/" + (duration * 1000));
         }
         try {
            terminated = executorService.awaitTermination(monitoringPeriod, TimeUnit.MILLISECONDS);
            if (expired && !executorService.isShutdown()) {
               if (log.isInfoEnabled()) {
                  log.info("Shutting down the executor service.");
               }
               executorService.shutdownNow();
               terminated = true;
            }

            final long cnt = runInfo.getIteration();

            if (!expired) {
               sendPack(cnt - lastValue);
            }

            // should we log a change?
            if (cnt != lastValue) {
               lastValue = cnt;
               float averageSpeed = getSpeed(cnt);

               if (warmUpEnabled && !isMeasuring) {
                  float relDelta = averageSpeed / lastSpeed - 1f;
                  float absDelta = averageSpeed - lastSpeed;
                  if (log.isDebugEnabled()) {
                     log.debug("AverageSpeed: " + averageSpeed + ", LastSpeed: " + lastSpeed);
                     log.debug("Difference: " + absDelta + " (" + relDelta + "%)");
                  }
                  if ((getDurationInMillis() > minimalWarmUpDuration) && (lastValue > minimalWarmUpCount) && (Math.abs(absDelta) < 0.5f || Math.abs(relDelta) < 0.005f)) {
                     isMeasuring = true;
                     postWarmUp();
                     lastValue = 0;
                  }
                  lastSpeed = averageSpeed;
               }
            }
         } catch (InterruptedException ie) {
            ie.printStackTrace();
            // "Shit happens!", Forrest Gump
         }
         if (isMeasuring) {
            runTime = getDurationInMillis();
         }
      }

      setStopTime();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.generator.AbstractMessageGenerator#postWarmUp()
    */
   @Override
   protected void postWarmUp() throws Exception {
      if (log.isInfoEnabled()) {
         log.info("Server is warmed up - starting to measure...");
      }
      setStartTime();
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
    *           The monitoringPeriod to set.
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
    *           The thread queue size.
    */
   public void setThreadQueueSize(final int threadQueueSize) {
      this.threadQueueSize = threadQueueSize;
   }

   @Override
   protected void validateRunInfo() {
      assert runInfo.getDuration().getPeriodType() == PeriodType.TIME : this.getClass().getName() + " can only be used with an iteration based run configuration.";
   }
}
