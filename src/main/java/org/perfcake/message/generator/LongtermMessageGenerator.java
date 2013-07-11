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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.perfcake.message.Message;
import org.perfcake.message.MessageToSend;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;

/**
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class LongtermMessageGenerator extends AbstractMessageGenerator {

   private static final Logger log = Logger.getLogger(LongtermMessageGenerator.class);

   protected AtomicLong counter = new AtomicLong(0);
   private ExecutorService es;

//   protected boolean showCurrentSpeed = true;
//   protected boolean showAverageSpeed = true;
//   protected boolean showMessageSentCount = true;
   protected long monitoringPeriod = 1000; // default
   protected long duration = 60; // default, 60 seconds
   protected boolean measureResponseTime = false;
   protected int threadQueueSize = 1000; // default

   @Override
   public void init(MessageSenderManager messageSenderManager, List<MessageToSend> messageStore) throws Exception {
      super.init(messageSenderManager, messageStore);
   }

   @Override
   public void setReportManager(ReportManager reportManager) {
      this.reportManager = reportManager;
      reportManager.getTestRunInfo().setTestDuration(duration);
   }

   protected float getSpeed(long count) {
      long now = (stop == -1) ? System.currentTimeMillis() : stop;
      return 1000f * count / (now - start);
   }

   private void sendPack(long count) {
      for (long i = 0; i < count; i++) {
         es.submit(new SenderTask(reportManager, counter, messageSenderManager, messageStore));
      }
   }

   @Override
   public void generate() throws Exception {

      isMeasuring = !warmUpEnabled;
      setStartTime();

      if (log.isInfoEnabled()) {
         log.info("Preparing senders");
      }
      es = Executors.newFixedThreadPool(threads);
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

      while (!(expired = (runTime > duration * 1000)) || !terminated) {
         if (log.isDebugEnabled()) {
            log.debug("Run time: " + runTime + "/" + (duration * 1000));
         }
         try {
            terminated = es.awaitTermination(monitoringPeriod, TimeUnit.MILLISECONDS);
            if (expired && !es.isShutdown()) {
               if (log.isInfoEnabled()) {
                  log.info("Shutting down the executor service.");
               }
               es.shutdownNow();
               terminated = true;
            }

            final long cnt = counter.get();

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

   @Override
   protected void postWarmUp() throws Exception {
      if (log.isInfoEnabled()) {
         log.info("Server is warmed up - starting to measure...");
      }
      setStartTime();
      counter.set(0);
   }

   private static class SenderTask implements Runnable {

      private MessageSenderManager senderManager;

      private List<MessageToSend> messageStore;

      private AtomicLong counter;

      private ReportManager reportManager;

      public SenderTask(ReportManager rm, AtomicLong counter, MessageSenderManager senderManager, List<MessageToSend> messageStore) {
         this.reportManager = rm;
         this.senderManager = senderManager;
         this.messageStore = messageStore;
         this.counter = counter;
      }

      @Override
      public void run() {
         MessageSender sender = null;
         try {
            Iterator<MessageToSend> iterator = messageStore.iterator();
            while (iterator.hasNext()) {
               MessageToSend messageToSend = iterator.next();
               Message currentMessage = messageToSend.getMessage();
               long multiplicity = messageToSend.getMultiplicity();
               sender = senderManager.acquireSender();
               for (int i = 0; i < multiplicity; i++) {
                  sender.send(currentMessage);
               }
               senderManager.releaseSender(sender);
               sender = null;
            }
            reportManager.reportIteration();
            counter.incrementAndGet();
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            if (sender != null) {
               senderManager.releaseSender(sender);
            }
         }
      }
   }

   public long getMonitoringPeriod() {
      return monitoringPeriod;
   }

   public void setMonitoringPeriod(long monitoringPeriod) {
      this.monitoringPeriod = monitoringPeriod;
   }

   public long getDuration() {
      return duration;
   }

   public void setDuration(long duration) {
      this.duration = duration;
   }

   public boolean isMeasureResponseTime() {
      return measureResponseTime;
   }

   public void setMeasureResponseTime(boolean measureResponseTime) {
      this.measureResponseTime = measureResponseTime;
   }

   public int getThreadQueueSize() {
      return threadQueueSize;
   }

   public void setThreadQueueSize(int threadQueueSize) {
      this.threadQueueSize = threadQueueSize;
   }
}
