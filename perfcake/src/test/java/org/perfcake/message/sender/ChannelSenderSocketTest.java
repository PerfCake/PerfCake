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
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Properties;

/**
 * Tests {@link org.perfcake.message.sender.ChannelSenderSocket}.
 *
 * @author <a href="mailto:domin.hanak@gmail.com">Dominik Hanák</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class ChannelSenderSocketTest {

   private static final String PAYLOAD = "fish";
   private static final int PORT = 4444;
   private static String host;
   private String target;
   private final EchoSocketVerticle vert = new EchoSocketVerticle();

   @BeforeClass
   public void setUp() throws Exception {
      host = InetAddress.getLocalHost().getHostAddress();
      target = host + ":" + PORT;

      final Vertx vertx = VertxFactory.newVertx();
      vert.setVertx(vertx);
      vert.start();
   }

   @AfterClass
   public void tearDown() {
      vert.stop();
   }

   @Test
   public void testNormalMessage() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", target);
      senderProperties.setProperty("awaitResponse", "true");

      final Message message = new Message();
      message.setPayload(PAYLOAD);

      try {
         final ChannelSender sender = (ChannelSenderSocket) ObjectFactory.summonInstance(ChannelSenderSocket.class.getName(), senderProperties);

         sender.init();
         sender.preSend(message, null, null);

         final Serializable response = sender.doSend(message, null, null);
         Assert.assertEquals(response, "fish");

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
         final ChannelSender sender = (ChannelSenderSocket) ObjectFactory.summonInstance(ChannelSenderSocket.class.getName(), senderProperties);

         sender.init();
         sender.preSend(null, null, null);

         final Serializable response = sender.doSend(null, null, null);
         Assert.assertNull(response);

         sender.postSend(null);
      } catch (final Exception e) {
         Assert.fail(e.getMessage(), e.getCause());
      }
   }

   static class EchoSocketVerticle extends Verticle {
      @Override
      public void start() {
         vertx.createNetServer().connectHandler(new Handler<NetSocket>() {
            public void handle(final NetSocket sock) {
               sock.dataHandler(new Handler<Buffer>() {
                  public void handle(final Buffer buffer) {
                     sock.write(buffer);
                  }
               });
            }
         }).listen(PORT, host);
      }
   }
}
