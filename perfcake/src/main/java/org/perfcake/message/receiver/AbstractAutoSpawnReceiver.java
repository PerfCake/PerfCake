/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package org.perfcake.message.receiver;

import org.perfcake.PerfCakeException;
import org.perfcake.message.correlator.Correlator;
import org.perfcake.message.generator.DefaultMessageGenerator;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simplifies writing of the receivers. An implementation just needs to override its {@link Thread#run()} method.
 * The needed number of threads is started and terminated automatically. It is just necessary to pass the received
 * messages to the correlator using {@link Correlator#registerResponse(Serializable)}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public abstract class AbstractAutoSpawnReceiver extends AbstractReceiver implements Runnable {

   /**
    * Receiver's logger.
    */
   private static final Logger log = LogManager.getLogger(AbstractAutoSpawnReceiver.class);

   /**
    * Pool of receiver threads.
    */
   private ThreadPoolExecutor receiverThreads;

   @Override
   public abstract void run();

   @Override
   public void start() throws PerfCakeException {
      receiverThreads =  (ThreadPoolExecutor) Executors.newFixedThreadPool(threads, new ThreadFactoryBuilder()
            .setDaemon(true).setNameFormat("PerfCake-receiver-thread-%d").build());

      for (int i = 0; i < threads; i++) {
         try {
            receiverThreads.submit(getClass().newInstance());
         } catch (ReflectiveOperationException e) {
            throw new PerfCakeException("Unable to start receiver threads: ", e);
         }
      }
   }

   @Override
   public void stop() {
      receiverThreads.shutdownNow();
      try {
         receiverThreads.awaitTermination(500, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
         // no problem
      }

      int threadsAlive = receiverThreads.getActiveCount(); // get the number just once for it not to change between calls
      if (threadsAlive > 0) {
         log.warn(String.format("Unable to terminate all receiver threads. There are %d remaining threads alive.", threadsAlive));
      }
   }
}
