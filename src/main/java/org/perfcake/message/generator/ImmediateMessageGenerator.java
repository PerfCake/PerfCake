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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeConst;
import org.perfcake.message.Message;
import org.perfcake.message.MessageToSend;
import org.perfcake.message.ReceivedMessage;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.ValidatorManager;

/**
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class ImmediateMessageGenerator extends AbstractMessageGenerator {

   private static final Logger log = Logger.getLogger(ImmediateMessageGenerator.class);

   private AtomicInteger counter = new AtomicInteger(0);

   private AtomicLong responseTime = new AtomicLong(0);

   // private Queue<SpeedRecord> timeWindow = new
   // LinkedBlockingQueue<SpeedRecord>();
   private ExecutorService es;

   protected int timeWindowSize = 16; // default

   protected boolean messageNumberingEnabled = false;

   protected boolean measureResponseTime = false;

   protected Map<String, String> messageProperties = null;

   protected long count = 1; // default

   @Override
   public void setProperty(String property, String value) {
      if ("timeWindowSize".equals(property)) {
         timeWindowSize = Integer.valueOf(value);
      } else if ("messageNumberingEnabled".equals(property)) {
         messageNumberingEnabled = Boolean.valueOf(value);
      } else if ("measureResponseTime".equals(property)) {
         measureResponseTime = Boolean.valueOf(value);
      } else if ("count".equals(property)) {
         count = Long.valueOf(value);
      } else {
         super.setProperty(property, value);
      }
   }

   @Override
   public void setReportManager(ReportManager reportManager) {
      this.reportManager = reportManager;
      reportManager.getTestRunInfo().setTestIterations(count);
   }

   protected void updateStopTime() {
      stop = System.currentTimeMillis();
   }

   protected float getSpeed(int count) {
      long now = (stop == -1) ? System.currentTimeMillis() : stop;
      return 1000f * count / (now - start);
   }

   // private float getCurrentSpeed(int count) {
   // long now = System.currentTimeMillis();
   // long old = start;
   // long oldCount = 0;
   // if (!timeWindow.isEmpty()) {
   // if (timeWindow.size() < timeWindowSize) {
   // SpeedRecord l = timeWindow.peek();
   // old = l.getTime();
   // oldCount = l.getCount();
   // } else {
   // SpeedRecord l = timeWindow.poll();
   // old = l.getTime();
   // oldCount = l.getCount();
   // }
   // }
   // timeWindow.add(new SpeedRecord(now, (long) count));
   // return 1000f * (count - oldCount) / (now - old);
   // }
   //
   // private float getResponseTime() {
   // return ((float) responseTime.get() / (counter.get())) / 1000000;
   // }

   @Override
   public void generate() throws Exception {
      isMeasuring = !warmUpEnabled;
      setStartTime();

      if (log.isInfoEnabled()) {
         log.info("Preparing senders");
      }
      es = Executors.newFixedThreadPool(threads);
      for (int i = 0; i < count; i++) {
         es.submit(new SenderTask(reportManager, counter, responseTime, messageSenderManager, messageStore, messageNumberingEnabled, isMeasuring, count));
      }
      es.shutdown();

      boolean terminated = false;
      int lastValue = 0;
      float lastSpeed = Float.MIN_VALUE;
      float averageSpeed;

      if (warmUpEnabled && log.isInfoEnabled()) {
         log.info("Warming server up (for at least " + minimalWarmUpDuration + " ms and " + minimalWarmUpCount + " iterations" + ")");
      }
      while (!terminated) {
         try {
            terminated = es.awaitTermination(1, TimeUnit.SECONDS);

            // should we log a change?
            int cnt = counter.get();
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
         es.submit(new SenderTask(reportManager, counter, responseTime, messageSenderManager, messageStore, messageNumberingEnabled, isMeasuring, count));
      }
      es.shutdown();
   }

   private static class SenderTask implements Runnable {

      private MessageSenderManager senderManager;

      private List<MessageToSend> messageStore;

      private AtomicInteger counter;

      // private AtomicLong responseTime;
      private boolean messageNumberingEnabled;

      private boolean isMeasuring;

      private Map<String, String> messageProperties = null;

      private long count;

      private static AtomicBoolean firstSent = new AtomicBoolean(false);

      private ReportManager reportManager = null;

      // private static AtomicInteger messageNumber = new AtomicInteger(0);

      public SenderTask(ReportManager rm, AtomicInteger counter, AtomicLong resounseTime, MessageSenderManager senderManager, List<MessageToSend> messageStore, boolean messageNumberingEnabled, boolean isMeasuring, long count) {
         this.reportManager = rm;
         this.senderManager = senderManager;
         this.messageStore = messageStore;
         this.counter = counter;
         // this.responseTime = resounseTime;
         this.count = count;
         this.messageNumberingEnabled = messageNumberingEnabled;
         this.isMeasuring = isMeasuring;
      }

      @Override
      public void run() {
         MessageSender sender = null;
         ReceivedMessage receivedMessage = null;
         try {
            Iterator<MessageToSend> iterator = messageStore.iterator();
            messageProperties = new HashMap<String, String>();
            if (messageNumberingEnabled) {
               messageProperties.put(PerfCakeConst.MESSAGE_NUMBER_PROPERTY, String.valueOf(counter.get()));
               messageProperties.put(PerfCakeConst.COUNT_MESSAGE_PROPERTY, String.valueOf(count));
            }
            firstSent.set(false);
            while (iterator.hasNext()) {
               if (counter.get() == 0 && isMeasuring && !firstSent.get()) {
                  messageProperties.put(PerfCakeConst.PERFORMANCE_MESSAGE_PROPERTY, PerfCakeConst.START_VALUE);
                  firstSent.set(true);
               } else if (counter.get() == count - 1 && !firstSent.get()) {
                  messageProperties.put(PerfCakeConst.PERFORMANCE_MESSAGE_PROPERTY, PerfCakeConst.STOP_VALUE);
                  firstSent.set(true);
               }
               sender = senderManager.acquireSender();
               MessageToSend messageToSend = iterator.next();
               Message currentMessage = new Message(messageToSend.getMessage().getPayload());
               long multiplicity = messageToSend.getMultiplicity();

               for (int i = 0; i < multiplicity; i++) {
                  receivedMessage = new ReceivedMessage(sender.send(currentMessage, messageProperties), messageToSend.getValidatorId());
                  ValidatorManager.addToResultMessages(receivedMessage);
               }
               senderManager.releaseSender(sender); // !!! important !!!
               sender = null;
            }
            // responseTime.addAndGet(sender.getResponseTime());
            counter.incrementAndGet();
            reportManager.reportIteration();
            // sender.close();
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            if (sender != null) {
               senderManager.releaseSender(sender);
            }
         }
      }
   }
}
