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
import org.perfcake.reporting.ReportManager;

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

   /**
    * The number of iterations to generate.
    */
   protected long count = 1;

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.generator.AbstractMessageGenerator#setReportManager(org.perfcake.reporting.ReportManager)
    */
   @Override
   public void setReportManager(ReportManager reportManager) {
      super.setReportManager(reportManager);
      reportManager.getTestRunInfo().setTestIterations(count);
   }

   /**
    * Computes the current average speed the iterations are executed.
    * 
    * @param The
    *           iteration count.
    * @return The current average iteration execution speed.
    */
   protected float getSpeed(long cnt) {
      long now = (stop == -1) ? System.currentTimeMillis() : stop;
      return 1000f * cnt / (now - start);
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
      for (int i = 0; i < count; i++) {
         executorService.submit(new SenderTask(reportManager, counter, messageSenderManager, messageStore, messageNumberingEnabled, isMeasuring, count));
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
            long cnt = counter.get();
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
      counter.set(0);
      // responseTime.set(0);
      int cnt = waiting.size();
      int i = 0;
      for (; i < cnt; i++) {
         executorService.submit(waiting.get(i));
      }
      for (; i < count; i++) {
         executorService.submit(new SenderTask(reportManager, counter, messageSenderManager, messageStore, messageNumberingEnabled, isMeasuring, count));
      }
      executorService.shutdown();
   }

   /**
    * Used to read the number of iterations to send.
    * 
    * @return The number of iterations to send.
    */
   public long getCount() {
      return count;
   }

   /**
    * Sets the number of iterations to send.
    * 
    * @param count
    *           The number of iterations to send.
    */
   public void setCount(long count) {
      this.count = count;
   }
}
