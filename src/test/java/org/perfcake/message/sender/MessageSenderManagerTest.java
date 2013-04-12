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

package org.perfcake.message.sender;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.perfcake.PerfCakeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class MessageSenderManagerTest {
   private static MessageSenderManager msm;

   private static final int SENDER_COUNT = 100;

   private static final String SENDER_CLASS_NAME = "org.perfcake.message.sender.DummySender";

   private static final int THREAD_COUNT = 100;

   private static final int SENDER_TASK_COUNT = 2000;

   private static final int THREAD_SLEEP_MILLIS = 1000;

   private static final AtomicInteger counter = new AtomicInteger(SENDER_TASK_COUNT);

   private static Map<Runnable, Throwable> threads = new HashMap<Runnable, Throwable>();

   @BeforeClass
   public static void setUpClass() throws Exception {
      msm = new MessageSenderManager();
      msm.setProperty("sender-pool-size", String.valueOf(SENDER_COUNT));
      msm.setProperty("sender-class", SENDER_CLASS_NAME);
      msm.init();
   }

   @AfterClass
   public static void tearDownClass() throws Exception {
      msm.close();
   }

   @Test
   public void messageSenderManagerTest() throws Exception {
      assertTrue(msm.availableSenderCount() == SENDER_COUNT);
      MessageSender[] senders = new MessageSender[SENDER_COUNT];
      int i = 0, n = 5;
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
      assertTrue(senders[0] instanceof DummySender, "Sender is an instance of " + senders[0].getClass().getName() + ". It should be instance of " + SENDER_CLASS_NAME);
      assertTrue(msm.availableSenderCount() == 0);
      try {
         msm.acquireSender();
      } catch (PerfCakeException te) {
         assertTrue(te.getMessage().equals("MessageSender pool is empty."));
      }
      msm.releaseAllSenders();
      assertTrue(msm.availableSenderCount() == SENDER_COUNT);
   }

   @Test
   public void threadSafeTest() throws Exception {
      ExecutorService es = Executors.newFixedThreadPool(THREAD_COUNT);
      for (int i = 0; i < SENDER_TASK_COUNT; i++) {
         Runnable senderTask = new SenderTask();
         threads.put(senderTask, null);
         es.submit(senderTask);
      }
      es.shutdown();
      es.awaitTermination((long) (SENDER_TASK_COUNT * THREAD_SLEEP_MILLIS * 1.2), TimeUnit.MILLISECONDS);
      assertTrue(es.isTerminated());
      assertTrue(es.isShutdown());
      Iterator<Runnable> it = threads.keySet().iterator();
      while (it.hasNext()) {
         Throwable t = threads.get(it.next());
         if (t != null) {
            fail("One of the threads threw following exception: " + t.getMessage());
         }
      }
   }

   private static class SenderTask implements Runnable {
      private static final Random rnd = new Random(System.currentTimeMillis());

      @Override
      public void run() {
         try {
            MessageSender sender = msm.acquireSender();
            Thread.sleep(rnd.nextInt(THREAD_SLEEP_MILLIS));
            msm.releaseSender(sender);
            counter.decrementAndGet();
         } catch (Throwable t) {
            threads.put(Thread.currentThread(), t);
         }
      }
   }
}
