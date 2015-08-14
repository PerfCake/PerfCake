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

import org.apache.camel.spring.SpringCamelContext;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Properties;

/**
 * Tests {@link MqttSender}.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Test(groups = { "unit" })
public class MqttSenderTest {

   private static final String MESSAGE = "Test message";
   private static final String RESPONSE1 = "1:" + MESSAGE;
   private static final String RESPONSE2 = "2:" + MESSAGE;
   private SpringCamelContext camelCtx;

   @BeforeTest
   public void prepareCamel() {
      try {
         camelCtx = SpringCamelContext.springCamelContext("mqtt-sender-camel-context.xml");
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @AfterTest
   public void stopCamel() {
      if (camelCtx != null) {
         try {
            camelCtx.destroy();
         } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
         }
      }
   }

   @Test
   public void testPublishTwoWayOneBroker() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "tcp://127.0.0.1:61616/mqtt-topic-1");
      senderProperties.setProperty("responseTarget", "tcp://127.0.0.1:61616/mqtt-response-topic-1");
      _testSender(senderProperties, RESPONSE1);
   }

   @Test
   public void testPublishTwoWayDefaultBroker() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "tcp://127.0.0.1:61616/mqtt-topic-1");
      senderProperties.setProperty("responseTarget", "mqtt-response-topic-1");
      _testSender(senderProperties, RESPONSE1);
   }

   @Test
   public void testPublishTwoWayTwoBrokers() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "tcp://127.0.0.1:61616/mqtt-topic-2");
      senderProperties.setProperty("responseTarget", "tcp://127.0.0.1:62626/mqtt-response-topic-2");
      _testSender(senderProperties, RESPONSE2);
   }

   @Test
   public void testPublishOneWay() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "tcp://127.0.0.1:61616/mqtt-topic");
      _testSender(senderProperties, null);
   }

   private void _testSender(final Properties senderProperties, final String expectedResponse) {
      try {
         final MqttSender sender = (MqttSender) ObjectFactory.summonInstance(MqttSender.class.getName(), senderProperties);

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
      sender.preSend(message, additionalProperties, messageAttributes);
      response = (String) sender.send(message, additionalProperties, null);
      sender.postSend(message);
      sender.close();
      return response;
   }
}
