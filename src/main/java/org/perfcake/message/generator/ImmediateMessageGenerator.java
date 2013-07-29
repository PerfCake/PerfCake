/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.message.generator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.perfcake.common.PeriodType;
import org.perfcake.nreporting.ReportManager;

/**
 * <p>
 * Iteration count driven generator - generates a specified number of iterations. The actual number is specified by the value of {@link #count} with the default value of 1.
 * </p>
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class ImmediateMessageGenerator extends AbstractMessageGenerator {

   /**
    * The generator's logger.
    */
   private static final Logger log = Logger.getLogger(ImmediateMessageGenerator.class);

   /**
    * The properties that will be set on messages that are send.
    */
   protected Map<String, String> messageProperties = null;

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.generator.AbstractMessageGenerator#setReportManager(org.perfcake.reporting.ReportManager)
    */
   @Override
   public void setReportManager(final ReportManager reportManager) {
      super.setReportManager(reportManager);
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
      for (int i = 0; i < runInfo.getDuration().getPeriod(); i++) {
         executorService.submit(new SenderTask(reportManager, messageSenderManager, messageStore, messageNumberingEnabled, isMeasuring));
      }
      executorService.shutdown();

      boolean terminated = false;
      long lastValue = 0;
      float lastSpeed = Float.MIN_VALUE;
      float averageSpeed;

      if (warmUpEnabled && log.isInfoEnabled()) {
         log.info("Warming server up (for at least " + minimalWarmUpDuration + " ms and " + minimalWarmUpCount + " iterations" + ")");
      }
      while (!terminated) {
         try {
            terminated = executorService.awaitTermination(1, TimeUnit.SECONDS);

            // should we log a change?
            long cnt = runInfo.getIteration();
            if (cnt != lastValue) {
               lastValue = cnt;

               if (warmUpEnabled && !isMeasuring) {
                  averageSpeed = getSpeed(cnt);
                  float relDelta = averageSpeed / lastSpeed - 1f;
                  float absDelta = averageSpeed - lastSpeed;
                  if ((getDurationInMillis() > minimalWarmUpDuration) && (lastValue > minimalWarmUpCount) && (Math.abs(absDelta) < 0.5f || Math.abs(relDelta) < 0.005f)) {
                     isMeasuring = true;
                     postWarmUp();
                  }
                  lastSpeed = averageSpeed;
               }
            }
         } catch (InterruptedException ie) {
            // "Shit happens!", Forrest Gump
         } catch (Exception e) {
            e.printStackTrace();
            executorService.shutdownNow();
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
      executorService.shutdown();
      List<Runnable> waiting = executorService.shutdownNow();
      messageSenderManager.releaseAllSenders();
      executorService.awaitTermination(30, TimeUnit.SECONDS);
      executorService = Executors.newFixedThreadPool(threads);
      reportManager.reset(); // TODO this resets runInfo as well, this should be probably done differently once the warm-up handling is updated
      int cnt = waiting.size();
      int i = 0;
      for (; i < cnt; i++) {
         executorService.submit(waiting.get(i));
      }
      for (; i < runInfo.getDuration().getPeriod(); i++) {
         executorService.submit(new SenderTask(reportManager, messageSenderManager, messageStore, messageNumberingEnabled, isMeasuring));
      }
      executorService.shutdown();
   }

   /**
    * Used to read the number of iterations to send.
    * 
    * @return The number of iterations to send.
    */
   public long getCount() {
      return runInfo.getDuration().getPeriod();
   }

   @Override
   protected void validateRunInfo() {
      assert runInfo.getDuration().getPeriodType() == PeriodType.ITERATION : this.getClass().getName() + " can only be used with an iteration based run configuration.";
   }
}
