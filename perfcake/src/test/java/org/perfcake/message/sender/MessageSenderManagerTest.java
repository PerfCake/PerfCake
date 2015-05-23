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
package org.perfcake.message.sender;

import static org.testng.Assert.*;

import org.perfcake.PerfCakeException;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests {@link org.perfcake.message.sender.MessageSenderManager}.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Test(groups = { "unit" })
public class MessageSenderManagerTest {

   private static final int SENDER_COUNT = 100;
   private static final String SENDER_CLASS_NAME = TestSender.class.getName();
   private static final int THREAD_COUNT = 100;
   private static final int SENDER_TASK_COUNT = 2000;
   private static final int THREAD_SLEEP_MILLIS = 1000;
   private static final AtomicInteger counter = new AtomicInteger(SENDER_TASK_COUNT);

   @Test
   public void messageSenderManagerTest() throws Exception {
      final MessageSenderManager msm = new MessageSenderManager();
      msm.setSenderPoolSize(SENDER_COUNT);
      msm.setSenderClass(SENDER_CLASS_NAME);
      msm.init();

      assertTrue(msm.availableSenderCount() == SENDER_COUNT);
      final MessageSender[] senders = new MessageSender[SENDER_COUNT];
      int i = 0;
      final int n = 5;
      for (; i < n; i++) {
         senders[i] = msm.acquireSender();
      }
      assertNotNull(senders[0]);
      assertTrue(msm.availableSenderCount() == (SENDER_COUNT - n));
      for (i = 0; i < n; i++) {
         msm.releaseSender(senders[i]);
      }
      assertTrue(msm.availableSenderCount() == SENDER_COUNT);
      for (i = 0; i < SENDER_COUNT; i++) {
         senders[i] = msm.acquireSender();
      }
      assertTrue(senders[0] instanceof TestSender, "Sender is an instance of " + senders[0].getClass().getName() + ". It should be instance of " + SENDER_CLASS_NAME);
      assertTrue(msm.availableSenderCount() == 0);
      try {
         msm.acquireSender();
      } catch (final PerfCakeException te) {
         assertTrue(te.getMessage().equals("MessageSender pool is empty."));
      }
      msm.releaseAllSenders();
      assertTrue(msm.availableSenderCount() == SENDER_COUNT);
      msm.close();
   }

   @Test(groups = { "ueber", "performance" })
   public void threadSafeTest() throws Exception {
      final MessageSenderManager msm = new MessageSenderManager();
      msm.setSenderPoolSize(SENDER_COUNT);
      msm.setSenderClass(SENDER_CLASS_NAME);
      msm.init();

      final Map<Runnable, Throwable> threads = new HashMap<Runnable, Throwable>();
      final ExecutorService es = Executors.newFixedThreadPool(THREAD_COUNT);

      for (int i = 0; i < SENDER_TASK_COUNT; i++) {
         final Runnable senderTask = new SenderTask(msm, threads);
         threads.put(senderTask, null);
         es.submit(senderTask);
      }
      es.shutdown();
      es.awaitTermination((long) (SENDER_TASK_COUNT * THREAD_SLEEP_MILLIS * 1.2), TimeUnit.MILLISECONDS);
      assertTrue(es.isTerminated());
      assertTrue(es.isShutdown());
      final Iterator<Runnable> it = threads.keySet().iterator();
      while (it.hasNext()) {
         final Throwable t = threads.get(it.next());
         if (t != null) {
            fail("One of the threads threw following exception: " + t.getMessage());
         }
      }
      msm.close();
   }

   private static class SenderTask implements Runnable {
      private static final Random rnd = new Random(System.currentTimeMillis());

      private final MessageSenderManager msm;
      private final Map<Runnable, Throwable> threads;

      public SenderTask(final MessageSenderManager msm, final Map<Runnable, Throwable> threads) {
         this.msm = msm;
         this.threads = threads;
      }

      @Override
      public void run() {
         try {
            final MessageSender sender = msm.acquireSender();
            Thread.sleep(rnd.nextInt(THREAD_SLEEP_MILLIS));
            msm.releaseSender(sender);
            counter.decrementAndGet();

         } catch (final Throwable t) {
            threads.put(Thread.currentThread(), t);
         }
      }
   }
}
