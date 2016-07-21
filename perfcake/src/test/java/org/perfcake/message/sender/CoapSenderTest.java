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

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Properties;

/**
 * Tests {@link CoapSender}.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Test(groups = { "unit" })
public class CoapSenderTest {

   private static final String MESSAGE = "Test message";
   private static final String RESPONSE_GET = "GET:";
   private static final String RESPONSE_POST = "POST:" + MESSAGE;
   private static final String RESPONSE_POST_ACK = "POST:ACK:" + MESSAGE;
   private static final String RESPONSE_PUT = "PUT:" + MESSAGE;
   private static final String RESPONSE_DELETE = "DELETE:";

   private CoapServer coapServer;

   @BeforeTest
   public void prepareCoapServer() {
      coapServer = new CoapServer();
      coapServer.add(new MyResource("test-resource"));
      coapServer.add(new OnlyPostResource("test-resource-only-post"));
      coapServer.start();
   }

   @AfterTest
   public void stopCoapServer() {
      coapServer.stop();
   }

   @Test
   public void testCoapSenderDefault() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource");
      _testSender(senderProperties, RESPONSE_POST);
   }

   @Test
   public void testCoapSenderConfirmableRequests() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource");
      senderProperties.setProperty("requestType", "confirmable");
      _testSender(senderProperties, RESPONSE_POST_ACK);
   }

   @Test
   public void testCoapSenderGet() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource");
      senderProperties.setProperty("method", "GET");
      _testSender(senderProperties, RESPONSE_GET);
   }

   @Test
   public void testCoapSenderPost() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource");
      senderProperties.setProperty("method", "POST");
      _testSender(senderProperties, RESPONSE_POST);
   }

   @Test
   public void testCoapSenderPut() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource");
      senderProperties.setProperty("method", "PUT");
      _testSender(senderProperties, RESPONSE_PUT);
   }

   @Test
   public void testCoapSenderDelete() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource");
      senderProperties.setProperty("method", "DELETE");
      _testSender(senderProperties, RESPONSE_DELETE);
   }

   @Test
   public void testCoapSenderOnlyPostGet() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource-only-post");
      senderProperties.setProperty("method", "GET");
      _testSender(senderProperties, "");
   }

   @Test
   public void testCoapSenderOnlyPostPost() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource-only-post");
      senderProperties.setProperty("method", "POST");
      _testSender(senderProperties, RESPONSE_POST);
   }

   @Test
   public void testCoapSenderOnlyPostDelete() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource-only-post");
      senderProperties.setProperty("method", "DELETE");
      _testSender(senderProperties, "");
   }

   @Test
   public void testCoapSenderOnlyPostPut() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "coap://127.0.0.1:5683/test-resource-only-post");
      senderProperties.setProperty("method", "PUT");
      _testSender(senderProperties, "");
   }

   private void _testSender(final Properties senderProperties, final String expectedResponse) {
      try {
         final CoapSender sender = (CoapSender) ObjectFactory.summonInstance(CoapSender.class.getName(), senderProperties);
         final String response = _sendMessage(sender, new Message(MESSAGE), null);
         Assert.assertEquals(response, expectedResponse);
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   private String _sendMessage(final MessageSender sender, final Message message, final Map<String, String> additionalProperties) throws Exception {
      return _sendMessage(sender, message, additionalProperties, null);
   }

   private String _sendMessage(final MessageSender sender, final Message message, final Map<String, String> additionalProperties, final Properties messageAttributes) throws Exception {
      String response;
      sender.init();
      sender.preSend(message, messageAttributes);
      response = (String) sender.send(message, null);
      sender.postSend(message);
      sender.close();
      return response;
   }

   private class MyResource extends CoapResource {
      public MyResource(final String name) {
         super(name);
      }

      @Override
      public void handleGET(final CoapExchange exchange) {
         exchange.respond("GET:");
      }

      @Override
      public void handlePOST(final CoapExchange exchange) {
         final StringBuffer response = new StringBuffer();
         response.append("POST:");
         if (exchange.advanced().getRequest().getType().equals(CoAP.Type.CON)) {
            response.append("ACK:");
            exchange.advanced().getRequest().setAcknowledged(true);
         }
         response.append(exchange.getRequestText());
         exchange.respond(response.toString());
      }

      @Override
      public void handlePUT(final CoapExchange exchange) {
         exchange.respond("PUT:" + exchange.getRequestText());
      }

      @Override
      public void handleDELETE(final CoapExchange exchange) {
         exchange.respond("DELETE:");
      }
   }

   private class OnlyPostResource extends CoapResource {
      public OnlyPostResource(final String name) {
         super(name);
      }

      @Override
      public void handlePOST(final CoapExchange exchange) {
         exchange.respond("POST:" + exchange.getRequestText());
      }
   }
}