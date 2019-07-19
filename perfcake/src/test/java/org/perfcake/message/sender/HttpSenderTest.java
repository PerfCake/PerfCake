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

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.message.Message;
import org.perfcake.message.sender.HttpSender.Method;
import org.perfcake.util.ObjectFactory;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;

/**
 * Tests {@link org.perfcake.message.sender.HttpSender}.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Test(groups = { "unit" })
public class HttpSenderTest extends TestSetup {

   private static final String URL_GET = "http://httpbin.org/get";
   private static final String URL_GET_RESPONSE_URL = "https://httpbin.org/get";
   private static final String URL_POST = "http://httpbin.org/post";
   private static final String URL_POST_RESPONSE_URL = "https://httpbin.org/post";

   private static final int RESPONSE_500_SIZE = 10;
   private static final String URL_STATUS_500 = "http://httpbin.org/drip?numbytes=" + RESPONSE_500_SIZE + "&duration=0&delay=0&code=500";

   private static final String TEST_HEADER_NAME = "Test-Header-Name";
   private static final String TEST_HEADER_VALUE = "test-header-value...";
   private static final String TEST_PROPERTY_NAME = "Test-Property-Name";
   private static final String TEST_PROPERTY_VALUE = "test-property-value...";
   private static final String TEST_ADDITIONAL_PROPERTY_NAME = "Test-Additional_Property-Name";
   private static final String TEST_ADDITIONAL_PROPERTY_VALUE = "test-Additional_property-value...";
   private static final String POST_PAYLOAD = "I'm the fish!";
   private static final String METHOD_PROPERTY = "method";
   private static final String METHOD_VALUE = "GET";

   @Test
   public void testGetMethod() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_GET);

      String response = null;
      try {
         final HttpSender sender = (HttpSender) ObjectFactory.summonInstance(HttpSender.class.getName(), senderProperties);
         Assert.assertEquals(sender.getTarget(), URL_GET);
         Assert.assertEquals(sender.getMethod(), Method.GET);
         Assert.assertNull(sender.getExpectedResponseCodes());

         final Properties additionalMessageProperties = new Properties();
         additionalMessageProperties.setProperty(TEST_ADDITIONAL_PROPERTY_NAME, TEST_ADDITIONAL_PROPERTY_VALUE);

         final Message noPayloadMessage = new Message();
         noPayloadMessage.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
         noPayloadMessage.setProperty(TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);

         response = _sendMessage(sender, noPayloadMessage, additionalMessageProperties);
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }

      Assert.assertNotNull(response);
      Assert.assertTrue(response.contains("\"url\": \"" + URL_GET_RESPONSE_URL + "\""));
      Assert.assertTrue(response.contains("\"" + TEST_HEADER_NAME + "\": \"" + TEST_HEADER_VALUE + "\""));
      Assert.assertTrue(response.contains("\"" + TEST_PROPERTY_NAME + "\": \"" + TEST_PROPERTY_VALUE + "\""));
   }

   @Test
   public void testDynamicMethod() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("dynamicMethod", "@{" + METHOD_PROPERTY + "}");
      senderProperties.setProperty("target", URL_GET);

      String response = null;
      try {
         final HttpSender sender = (HttpSender) ObjectFactory.summonInstance(HttpSender.class.getName(), senderProperties);

         final Properties messageAttributes = new Properties();
         messageAttributes.setProperty(METHOD_PROPERTY, METHOD_VALUE);

         final Message noPayloadMessage = new Message();

         response = _sendMessage(sender, noPayloadMessage, messageAttributes);

         Assert.assertEquals(sender.getDynamicMethod(messageAttributes), Method.valueOf(METHOD_VALUE));
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }

      Assert.assertNotNull(response);
      Assert.assertTrue(response.contains("\"url\": \"" + URL_GET_RESPONSE_URL + "\""));
   }

   @Test
   public void testNullMessage() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_GET);
      String response = null;
      try {
         response = _sendMessage((HttpSender) ObjectFactory.summonInstance(HttpSender.class.getName(), senderProperties), null, null);
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }

      Assert.assertNotNull(response);
      Assert.assertTrue(response.contains("\"url\": \"" + URL_GET_RESPONSE_URL + "\""));
   }

   @Test
   public void testPostMethod() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("method", "POST");
      senderProperties.setProperty("target", URL_POST);
      String response = null;
      try {
         final Message payloadMessage = new Message();
         payloadMessage.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
         payloadMessage.setPayload(POST_PAYLOAD);

         response = _sendMessage((HttpSender) ObjectFactory.summonInstance(HttpSender.class.getName(), senderProperties), payloadMessage, null);
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);
      Assert.assertTrue(response.contains("\"url\": \"" + URL_POST_RESPONSE_URL + "\""));
      Assert.assertTrue(response.contains("\"" + TEST_HEADER_NAME + "\": \"" + TEST_HEADER_VALUE + "\""));
   }

   @Test
   public void testResponseCode() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_STATUS_500);
      senderProperties.setProperty("expectedResponseCodes", "500");
      String response = null;
      try {
         final Message noPayloadMessage = new Message();
         noPayloadMessage.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
         noPayloadMessage.setProperty(TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);

         response = _sendMessage((HttpSender) ObjectFactory.summonInstance(HttpSender.class.getName(), senderProperties), noPayloadMessage, null);
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);
   }

   @Test
   public void testMultipleResponseCodes() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_STATUS_500);
      senderProperties.setProperty("expectedResponseCodes", "500,200");
      String response = null;
      try {
         final HttpSender sender = (HttpSender) ObjectFactory.summonInstance(HttpSender.class.getName(), senderProperties);
         final List<Integer> responseCodeList = sender.getExpectedResponseCodeList();
         Assert.assertNotNull(responseCodeList);
         Assert.assertEquals(responseCodeList.size(), 2);
         Assert.assertTrue(responseCodeList.contains(500));
         Assert.assertTrue(responseCodeList.contains(200));

         final Message noPayloadMessage = new Message();
         noPayloadMessage.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
         noPayloadMessage.setProperty(TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);

         response = _sendMessage(sender, noPayloadMessage, null);
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);

      senderProperties.setProperty("target", URL_GET);
      try {
         final Message noPayloadMessage = new Message();
         noPayloadMessage.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
         noPayloadMessage.setProperty(TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);

         response = _sendMessage((HttpSender) ObjectFactory.summonInstance(HttpSender.class.getName(), senderProperties), noPayloadMessage, null);
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);
   }

   @Test
   public void testInvalidResponseCode() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_STATUS_500);
      senderProperties.setProperty("expectedResponseCodes", "200");
      String response = null;
      try {
         final Message noPayloadMessage = new Message();
         noPayloadMessage.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
         noPayloadMessage.setProperty(TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);

         response = _sendMessage((HttpSender) ObjectFactory.summonInstance(HttpSender.class.getName(), senderProperties), noPayloadMessage, null);

         Assert.fail("Expected exception was not thrown.");
      } catch (final Exception e) {
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
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("method", "GET");
      senderProperties.setProperty("target", URL_STATUS_500);
      String response = null;
      try {
         final Message noPayloadMessage = new Message();
         noPayloadMessage.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
         noPayloadMessage.setProperty(TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);

         response = _sendMessage((HttpSender) ObjectFactory.summonInstance(HttpSender.class.getName(), senderProperties), noPayloadMessage, null);
      } catch (final Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(response);
      Assert.assertEquals(response.length(), RESPONSE_500_SIZE, "Response contains " + response.length() + "/" + RESPONSE_500_SIZE + " bytes.");
   }

   @Test
   public void testCookies() throws PerfCakeException, InterruptedException {
      final AtomicInteger atomicInteger = new AtomicInteger();
      final Vertx vertx = Vertx.vertx();
      final HttpServer server = vertx.createHttpServer();
      final Router router = Router.router(vertx);
      router.route("/*").handler(BodyHandler.create());
      router.route("/*").handler(CookieHandler.create());
      router.route("/*").handler((context) -> {
         if (context.getCookie("mysid") == null) {
            context.addCookie(Cookie.cookie("mysid", Integer.toString(atomicInteger.getAndIncrement())));
         }
         context.response().setStatusCode(200).end();
      });
      new Thread(() -> server.requestHandler(router::accept).listen(8091)).start();

      runScenario("test-cookies");

      server.close();

      Assert.assertEquals(atomicInteger.get(), 10); // each client maintained its own cookies
   }

   private String _sendMessage(final MessageSender sender, final Message message) throws Exception {
      return _sendMessage(sender, message, null);
   }

   private String _sendMessage(final MessageSender sender, final Message message, final Properties messageAttributes) throws Exception {
      String response = null;
      sender.init();
      sender.preSend(message, messageAttributes);
      response = (String) sender.send(message, null);
      sender.postSend(message);
      sender.close();
      return response;
   }
}
