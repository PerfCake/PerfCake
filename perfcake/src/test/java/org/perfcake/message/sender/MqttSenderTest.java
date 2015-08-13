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

import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.message.sender.HttpSender.Method;
import org.perfcake.util.ObjectFactory;

import org.apache.camel.spring.SpringCamelContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.HashMap;
import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * Tests {@link HttpSender}.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Test(groups = { "unit" })
public class MqttSenderTest {

   @BeforeClass
   public void prepareCamel() {
      try {
         SpringCamelContext ctx = SpringCamelContext.springCamelContext("mqtt-sender-camel-context.xml");

      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testPublish() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "tcp://localhost:1883/cpt-mqtt-topic");
      senderProperties.setProperty("responseTarget", "tcp://localhost:1883/cpt-mqtt-response-topic");

      String response = null;
      try {
         final MqttSender sender = (MqttSender) ObjectFactory.summonInstance(MqttSender.class.getName(), senderProperties);

         response = _sendMessage(sender, new Message("test"), null);
         System.out.println(response);
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   private String _sendMessage(final MessageSender sender, final Message message, final Map<String, String> additionalProperties) throws Exception {
      return _sendMessage(sender, message, additionalProperties, null);
   }

   private String _sendMessage(final MessageSender sender, final Message message, final Map<String, String> additionalProperties, final Properties messageAttributes) throws Exception {
      String response = null;
      sender.init();
      sender.preSend(message, additionalProperties, messageAttributes);
      response = (String) sender.send(message, additionalProperties, null);
      sender.postSend(message);
      sender.close();
      return response;
   }

}
