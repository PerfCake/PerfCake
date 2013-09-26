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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.message.sender.HTTPSender.Method;
import org.perfcake.util.ObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class HTTPSenderTest {
   private final Properties senderProperties = new Properties();
   private final Map<String, String> additionalMessageProperties = new HashMap<>();

   private static final String URL_GET = "http://httpbin.org/get";
   private static final String URL_POST = "http://httpbin.org/post";
   private static final String URL_STATUS_500 = "http://httpbin.org/status/:500";

   private static final String TEST_HEADER_NAME = "Test-Header-Name";
   private static final String TEST_HEADER_VALUE = "test-header-value...";
   private static final String TEST_PROPERTY_NAME = "Test-Property-Name";
   private static final String TEST_PROPERTY_VALUE = "test-property-value...";
   private static final String TEST_ADDITIONAL_PROPERTY_NAME = "Test-Additional_Property-Name";
   private static final String TEST_ADDITIONAL_PROPERTY_VALUE = "test-Additional_property-value...";
   private static final String POST_PAYLOAD = "I'm the fish!";

   private Message payloadMessage, noPayloadMessage;

   @BeforeClass
   public void prepare() {
      noPayloadMessage = new Message();
      noPayloadMessage.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
      noPayloadMessage.setProperty(TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);

      payloadMessage = new Message();
      payloadMessage.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
      payloadMessage.setPayload(POST_PAYLOAD);

      additionalMessageProperties.put(TEST_ADDITIONAL_PROPERTY_NAME, TEST_ADDITIONAL_PROPERTY_VALUE);
   }

   @BeforeTest
   public void clearProperties() {
      senderProperties.clear();
   }

   @Test
   public void testGetMethod() {
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_GET);

      String response = null;
      try {
         HTTPSender sender = (HTTPSender) ObjectFactory.summonInstance(HTTPSender.class.getName(), senderProperties);
         Assert.assertEquals(sender.getTarget(), URL_GET);
         Assert.assertEquals(sender.getMethod(), Method.GET);
         Assert.assertNull(sender.getExpectedResponseCodes());

         response = _sendMessage(sender, noPayloadMessage, additionalMessageProperties);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }

      Assert.assertNotNull(response);
      Assert.assertTrue(response.contains("\"url\": \"" + URL_GET + "\""));
      Assert.assertTrue(response.contains("\"" + TEST_HEADER_NAME + "\": \"" + TEST_HEADER_VALUE + "\""));
      Assert.assertTrue(response.contains("\"" + TEST_PROPERTY_NAME + "\": \"" + TEST_PROPERTY_VALUE + "\""));
   }

   @Test
   public void testNullMessage() {
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_GET);
      String response = null;
      try {
         response = _sendMessage((HTTPSender) ObjectFactory.summonInstance(HTTPSender.class.getName(), senderProperties), null, null);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }

      Assert.assertNotNull(response);
      Assert.assertTrue(response.contains("\"url\": \"" + URL_GET + "\""));
   }

   @Test
   public void testPostMethod() {
      senderProperties.setProperty("method", "POST");
      senderProperties.setProperty("target", URL_POST);
      String response = null;
      try {
         response = _sendMessage((HTTPSender) ObjectFactory.summonInstance(HTTPSender.class.getName(), senderProperties), payloadMessage, null);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);
      Assert.assertTrue(response.contains("\"url\": \"" + URL_POST + "\""));
      Assert.assertTrue(response.contains("\"" + TEST_HEADER_NAME + "\": \"" + TEST_HEADER_VALUE + "\""));
   }

   @Test
   public void testResponseCode() {
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_STATUS_500);
      senderProperties.setProperty("expectedResponseCodes", "500");
      String response = null;
      try {
         response = _sendMessage((HTTPSender) ObjectFactory.summonInstance(HTTPSender.class.getName(), senderProperties), noPayloadMessage, null);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);
   }

   @Test
   public void testMultipleResponseCodes() {
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_STATUS_500);
      senderProperties.setProperty("expectedResponseCodes", "500,200");
      String response = null;
      try {
         HTTPSender sender = (HTTPSender) ObjectFactory.summonInstance(HTTPSender.class.getName(), senderProperties);
         List<Integer> responseCodeList = sender.getExpectedResponseCodeList();
         Assert.assertNotNull(responseCodeList);
         Assert.assertEquals(responseCodeList.size(), 2);
         Assert.assertTrue(responseCodeList.contains(500));
         Assert.assertTrue(responseCodeList.contains(200));
         response = _sendMessage(sender, noPayloadMessage, null);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);

      senderProperties.setProperty("target", URL_GET);
      try {
         response = _sendMessage((HTTPSender) ObjectFactory.summonInstance(HTTPSender.class.getName(), senderProperties), noPayloadMessage, null);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);
   }

   @Test
   public void testInvalidResponseCode() {
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_STATUS_500);
      senderProperties.setProperty("expectedResponseCodes", "200");
      String response = null;
      try {
         response = _sendMessage((HTTPSender) ObjectFactory.summonInstance(HTTPSender.class.getName(), senderProperties), noPayloadMessage, null);
      } catch (Exception e) {
         if (e instanceof PerfCakeException) {
            Assert.assertTrue(e.getMessage().contains("unexpected HTTP response code: 500"));
         } else {
            Assert.fail(e.getMessage());
         }
      }

      Assert.assertNull(response);
   }

   @Test
   public void testDefaultResponseCodesHandlingError() {
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_STATUS_500);
      String response = null;
      try {
         response = _sendMessage((HTTPSender) ObjectFactory.summonInstance(HTTPSender.class.getName(), senderProperties), noPayloadMessage, null);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);
      Assert.assertTrue(response.contains("500 Internal Server Error"));
   }

   private String _sendMessage(MessageSender sender, Message message, Map<String, String> additionalProperties) throws Exception {
      String response = null;
      sender.init();
      sender.preSend(message, additionalProperties);
      response = (String) sender.send(message, additionalProperties, null);
      sender.postSend(message);
      sender.close();
      return response;
   }
}
