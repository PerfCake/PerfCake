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
import org.perfcake.message.Message;
import org.perfcake.message.correlator.Correlator;
import org.perfcake.message.generator.SenderTask;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class HttpReceiverTest {

   private String ack;
   private String received;

   private Semaphore semaphore = new Semaphore(0);

   @Test
   public void receiverTest() throws PerfCakeException, InterruptedException {
      HttpReceiver rec = new HttpReceiver();

      rec.setThreads(10);
      rec.setCorrelator(new TestCorrelator());
      rec.setHttpResponse("All righty");
      rec.start();

      Vertx vertx = Vertx.vertx();
      HttpClient client = vertx.createHttpClient();
      client.post(8088, "localhost", "/random/uri", response -> {
         response.bodyHandler(body -> {
            ack = body.toString();
            semaphore.release();
         });
      }).end("Heyda!");

      semaphore.acquire(2);

      rec.stop();

      Assert.assertEquals(ack, "All righty");
      Assert.assertEquals(received, "Heyda!");
   }

   private class TestCorrelator implements Correlator {

      @Override
      public void registerRequest(final SenderTask senderTask, final Message message, final Properties messageAttributes) {

      }

      @Override
      public void registerResponse(final Serializable response) {
         received = response.toString();
         semaphore.release();
      }
   }
}