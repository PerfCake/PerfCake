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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.perfcake.reporting.ReportManager;

/**
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class ImmediateMessageGenerator extends AbstractMessageGenerator {

   private static final Logger log = Logger.getLogger(ImmediateMessageGenerator.class);
   private AtomicLong counter = new AtomicLong(0);
   private ExecutorService es;
   protected int timeWindowSize = 16; // default
   protected Map<String, String> messageProperties = null;
   @Override
   public void setReportManager(ReportManager reportManager) {
      this.reportManager = reportManager;
      reportManager.getTestRunInfo().setTestIterations(count);
   }

   protected void updateStopTime() {
      stop = System.currentTimeMillis();
   }

   protected float getSpeed(long cnt) {
      long now = (stop == -1) ? System.currentTimeMillis() : stop;
      return 1000f * cnt / (now - start);
   }

   @Override
   public void generate() throws Exception {
      isMeasuring = !warmUpEnabled;
      setStartTime();

      if (log.isInfoEnabled()) {
         log.info("Preparing senders");
      }
      es = Executors.newFixedThreadPool(threads);
      for (int i = 0; i < count; i++) {
         es.submit(new SenderTask(reportManager, counter, messageSenderManager, messageStore, messageNumberingEnabled, isMeasuring, count));
      }
      es.shutdown();

      boolean terminated = false;
      long lastValue = 0;
      float lastSpeed = Float.MIN_VALUE;
      float averageSpeed;

      if (warmUpEnabled && log.isInfoEnabled()) {
         log.info("Warming server up (for at least " + minimalWarmUpDuration + " ms and " + minimalWarmUpCount + " iterations" + ")");
      }
      while (!terminated) {
         try {
            terminated = es.awaitTermination(1, TimeUnit.SECONDS);

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
            es.shutdownNow();
         }

      }
      setStopTime();
   }

   @Override
   protected void postWarmUp() throws Exception {
      if (log.isInfoEnabled()) {
         log.info("Server is warmed up - starting to measure...");
      }
      setStartTime();
      es.shutdown();
      List<Runnable> waiting = es.shutdownNow();
      messageSenderManager.releaseAllSenders();
      es.awaitTermination(30, TimeUnit.SECONDS);
      es = Executors.newFixedThreadPool(threads);
      counter.set(0);
      // responseTime.set(0);
      int cnt = waiting.size();
      int i = 0;
      for (; i < cnt; i++) {
         es.submit(waiting.get(i));
      }
      for (; i < count; i++) {
         es.submit(new SenderTask(reportManager, counter, messageSenderManager, messageStore, messageNumberingEnabled, isMeasuring, count));
      }
      es.shutdown();
   }

   public int getTimeWindowSize() {
      return timeWindowSize;
   }

   public void setTimeWindowSize(int timeWindowSize) {
      this.timeWindowSize = timeWindowSize;
   }
}
