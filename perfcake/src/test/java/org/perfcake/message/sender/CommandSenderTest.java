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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tests {@link org.perfcake.message.sender.CommandSender}.
 *
 * Dependent on BASH script greetings.sh which acts as a mock application.
 * Sends commands to the script to achieve 100% code coverage of all semantically
 * reachable basic blocks.
 *
 * Testing criterion: edge-pair coverage
 *
 * @author <a href="mailto:karasek.jose@gmail.com">Josef Karásek</a>
 */
@Test(groups = { "unit" })
public class CommandSenderTest {

   private static String scriptFile;

   @BeforeClass
   public static void determineOS() {
      if (System.getProperty("os.name").toLowerCase().contains("windows")) {
         scriptFile = "greeting.bat";
      } else {
         scriptFile = "greeting.sh";
      }
   }

   @Test
   public void nullMessageNoPayloadFakeArgumentTest() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "./src/test/resources/" + scriptFile + " Pepo");
      final Message message = null;
      String response = null;
      try {
         final CommandSender sender = (CommandSender) ObjectFactory.summonInstance(CommandSender.class.getName(), senderProperties);
         final Map<String, String> additionalMessageProperties = new HashMap<>();

         response = _sendMessage(sender, message, additionalMessageProperties);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertEquals(response.trim(), "Greetings Pepo! From ARG #1.");
   }

   @Test
   public void emptyMessageNoPayloadFakeArgumentTest() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "./src/test/resources/" + scriptFile + " Pepo");
      final Message message = new Message();
      String response = null;
      try {
         final CommandSender sender = (CommandSender) ObjectFactory.summonInstance(CommandSender.class.getName(), senderProperties);
         final Map<String, String> additionalMessageProperties = new HashMap<>();

         response = _sendMessage(sender, message, additionalMessageProperties);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertEquals(response.trim(), "Greetings Pepo! From ARG #1.");
   }

   @Test
   public void messageWithPayloadFromStdinTest() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "./src/test/resources/" + scriptFile);
      final Message message = new Message();
      message.setPayload("Pepo");
      String response = null;
      try {
         final CommandSender sender = (CommandSender) ObjectFactory.summonInstance(CommandSender.class.getName(), senderProperties);
         final Map<String, String> additionalMessageProperties = new HashMap<>();

         response = _sendMessage(sender, message, additionalMessageProperties);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertEquals(response.trim(), "Greetings Pepo! From STDIN.");
   }

   @Test
   public void messageWithPayloadFromArgumentTest() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "./src/test/resources/" + scriptFile);
      senderProperties.setProperty("messageFrom", "ARGUMENTS");
      final Message message = new Message();
      message.setPayload("Pepo");
      String response = null;
      try {
         final CommandSender sender = (CommandSender) ObjectFactory.summonInstance(CommandSender.class.getName(), senderProperties);
         final Map<String, String> additionalMessageProperties = new HashMap<>();

         response = _sendMessage(sender, message, additionalMessageProperties);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertEquals(response.trim(), "Greetings Pepo! From ARG #1.");
   }

   @Test
   public void messageWithPayloadFromArgumentWithGlobalPropertyTest() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "./src/test/resources/" + scriptFile);
      senderProperties.setProperty("messageFrom", "ARGUMENTS");
      final Message message = new Message();
      message.setPayload("Pepo");
      String response = null;
      try {
         final CommandSender sender = (CommandSender) ObjectFactory.summonInstance(CommandSender.class.getName(), senderProperties);
         final Map<String, String> additionalMessageProperties = new HashMap<>();
         additionalMessageProperties.put("TEST_VARIABLE", "testing");
         response = _sendMessage(sender, message, additionalMessageProperties);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertEquals(response.trim(), "Greetings Pepo! From ARG #1. TEST_VARIABLE=testing.");
   }

   @Test
   public void messageWithHeaderAndPayloadFromArgumentTest() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "./src/test/resources/" + scriptFile);
      senderProperties.setProperty("messageFrom", "ARGUMENTS");
      final Message message = new Message();
      message.setPayload("Pepo");
      message.setHeader("TEST_VARIABLE", "testing");
      String response = null;
      try {
         final CommandSender sender = (CommandSender) ObjectFactory.summonInstance(CommandSender.class.getName(), senderProperties);
         final Map<String, String> additionalMessageProperties = new HashMap<>();
         response = _sendMessage(sender, message, additionalMessageProperties);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertEquals(response.trim(), "Greetings Pepo! From ARG #1. TEST_VARIABLE=testing.");
   }

   @Test
   public void messageWithPropertyAndPayloadFromArgumentTest() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", "./src/test/resources/" + scriptFile);
      senderProperties.setProperty("messageFrom", "ARGUMENTS");
      final Message message = new Message();
      message.setPayload("Pepo");
      message.setProperty("TEST_VARIABLE", "testing");
      String response = null;
      try {
         final CommandSender sender = (CommandSender) ObjectFactory.summonInstance(CommandSender.class.getName(), senderProperties);
         final Map<String, String> additionalMessageProperties = new HashMap<>();
         response = _sendMessage(sender, message, additionalMessageProperties);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertEquals(response.trim(), "Greetings Pepo! From ARG #1. TEST_VARIABLE=testing.");
   }

   private String _sendMessage(final CommandSender sender, final Message message, final Map<String, String> additionalProperties) throws Exception {
      String response = null;
      sender.init();
      sender.preSend(message, additionalProperties, null);
      response = (String) sender.send(message, additionalProperties, null);
      sender.postSend(message);
      sender.close();
      return response;
   }
}
