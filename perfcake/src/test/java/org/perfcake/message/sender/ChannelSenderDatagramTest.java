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
package org.perfcake.message.sender;

import org.perfcake.message.Message;
import org.perfcake.util.ObjectFactory;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;

/**
 * Tests {@link org.perfcake.message.sender.ChannelSenderDatagram}.
 *
 * @author <a href="mailto:domin.hanak@gmail.com">Dominik Hanák</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class ChannelSenderDatagramTest {

   private static final String PAYLOAD = "fish";
   private static final int PORT = 4444;
   private static final String HOST = "127.0.0.1";
   private String target;
   final private Semaphore s = new Semaphore(0);
   final private DatagramSocketVerticle vert = new DatagramSocketVerticle(s);
   final private Vertx vertx = Vertx.vertx();

   @BeforeClass
   public void setUp() throws Exception {
      target = HOST + ":" + PORT;
      vertx.deployVerticle(vert);
      s.acquire(); // wait for deployment
   }

   @AfterClass
   public void tearDown() {
      vertx.close();
   }

   @Test(enabled = true)
   public void testNormalMessage() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", target);
      senderProperties.setProperty("awaitResponse", "true");
      senderProperties.setProperty("maxResponseSize", "5");

      final Message message = new Message();
      message.setPayload(PAYLOAD);

      try {
         final ChannelSender sender = (ChannelSenderDatagram) ObjectFactory.summonInstance(ChannelSenderDatagram.class.getName(), senderProperties);

         Assert.assertEquals(sender.getAwaitResponse(), true);
         Assert.assertEquals(sender.maxResponseSize, 5);

         sender.init();
         sender.preSend(message, null);

         final Serializable response = sender.doSend(message, null);
         Assert.assertEquals(response, "fish2");

         sender.postSend(message);
      } catch (final Exception e) {
         Assert.fail(e.getMessage(), e.getCause());
      }
   }

   @Test
   public void testNullMessage() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", target);

      try {
         final ChannelSender sender = (ChannelSenderDatagram) ObjectFactory.summonInstance(ChannelSenderDatagram.class.getName(), senderProperties);

         sender.init();
         sender.preSend(null, null);

         final Serializable response = sender.doSend(null, null);
         Assert.assertNull(response);

         sender.postSend(null);
      } catch (final Exception e) {
         Assert.fail(e.getMessage(), e.getCause());
      }
   }

   static class DatagramSocketVerticle extends AbstractVerticle {

      private final Semaphore s;

      public DatagramSocketVerticle(final Semaphore s) {
         this.s = s;
      }

      @Override
      public void start() {
         final DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions().setIpV6(false));
         socket.listen(PORT, HOST, asyncResult -> {
            if (asyncResult.succeeded()) {
               socket.handler(packet -> socket.send(packet.data().appendString("2"), packet.sender().port(), packet.sender().host(), (AsyncResultHandler<DatagramSocket>) asyncResult1 -> {
                  if (!asyncResult1.succeeded()) {
                     throw new IllegalStateException("Cannot send test response: ", asyncResult1.cause());
                  }
               }));
               s.release();
            } else {
               throw new IllegalStateException("Listen failed: " + HOST + ":" + PORT, asyncResult.cause());
            }
         });
      }
   }

}
