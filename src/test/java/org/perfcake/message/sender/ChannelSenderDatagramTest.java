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

import org.perfcake.message.Message;
import org.perfcake.util.ObjectFactory;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.datagram.DatagramPacket;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.datagram.InternetProtocolFamily;
import org.vertx.java.platform.Verticle;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Properties;

/**
 * @author Dominik Hanák <domin.hanak@gmail.com>
 * @author Martin Večera <marvenec@gmail.com>
 */
public class ChannelSenderDatagramTest {

   private static final String PAYLOAD = "fish";
   private static final int PORT = 4444;
   private static String host;
   private String target;
   private DatagramSocketVerticle vert = new DatagramSocketVerticle();

   @BeforeClass
   public void setUp() throws Exception {
      host = InetAddress.getLocalHost().getHostAddress();
      target = host + ":" + PORT;

      Vertx vertx = VertxFactory.newVertx();
      vert.setVertx(vertx);
      vert.start();
   }

   @AfterClass
   public void tearDown() {
      vert.stop();
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

         Serializable response = sender.doSend(message, null, null);
         Assert.assertEquals(response, "fish2");

         sender.postSend(message);
      } catch (Exception e) {
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

         Serializable response = sender.doSend(null, null, null);
         Assert.assertNull(response);

         sender.postSend(null);
      } catch (Exception e) {
         Assert.fail(e.getMessage(), e.getCause());
      }
   }

   static class DatagramSocketVerticle extends Verticle {
      @Override
      public void start() {
         final DatagramSocket socket = vertx.createDatagramSocket(InternetProtocolFamily.IPv4);
         final DatagramSocket responseSocket = vertx.createDatagramSocket(InternetProtocolFamily.IPv4);
         socket.listen(host, PORT, new AsyncResultHandler<DatagramSocket>() {
            public void handle(AsyncResult<DatagramSocket> asyncResult) {
               if (asyncResult.succeeded()) {
                  socket.dataHandler(new Handler<DatagramPacket>() {
                     public void handle(DatagramPacket packet) {
                        socket.send(packet.data().appendString("2"), packet.sender().getHostName(), packet.sender().getPort(), new AsyncResultHandler<DatagramSocket>() {
                           public void handle(AsyncResult<DatagramSocket> asyncResult) {
                              if (!asyncResult.succeeded()) {
                                 throw new IllegalStateException("Cannot send test response: ", asyncResult.cause());
                              }
                           }
                        });
                     }
                  });
               } else {
                  throw new IllegalStateException("Listen failed: ", asyncResult.cause());
               }
            }
         });
      }
   }

}
