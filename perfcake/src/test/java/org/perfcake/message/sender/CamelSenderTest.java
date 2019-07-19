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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "unit")
public class CamelSenderTest extends TestSetup {

   public static final String TEST_RESPONSE = "A toto je super odpověď!";

   @Test
   public void basicTest() throws PerfCakeException {
      final Vertx vertx = Vertx.vertx();
      final HttpServer server = vertx.createHttpServer();
      final Router router = Router.router(vertx);
      final List<List<String>> results = new ArrayList<>();
      router.route("/*").handler(BodyHandler.create());
      router.route("/*").handler((context) -> {
         results.add(Arrays.asList(context.request().absoluteURI(), context.request().method().toString(), context.getBodyAsString()));
         context.response().setStatusCode(200).end(TEST_RESPONSE);
      });
      new Thread(() -> server.requestHandler(router::accept).listen(8283)).start();

      runScenario("test-camel");

      server.close();

      Assert.assertEquals(results.size(), 100);
      Assert.assertEquals(results.get(0).get(0), "http://127.0.0.1:8283/perfcake?param1=value1");
      Assert.assertEquals(results.get(0).get(1), "POST");
      Assert.assertEquals(results.get(0).get(2), "Hello from Camel");
   }

   @Test
   public void pureSenderTest() throws Exception {
      final List<List<String>> results = new ArrayList<>();
      final Vertx vertx = Vertx.vertx();
      final HttpServer server = vertx.createHttpServer();
      final Router router = Router.router(vertx);
      router.route("/*").handler(BodyHandler.create());
      router.route("/*").handler((context) -> {
         results.add(Arrays.asList(context.request().absoluteURI(), context.request().method().toString(), context.getBodyAsString()));
         context.response().setStatusCode(201).end(TEST_RESPONSE);
      });
      new Thread(() -> server.requestHandler(router::accept).listen(8284)).start();

      final CamelSender sender = new CamelSender();
      sender.setTarget("http:127.0.0.1:8284/perfcake?param1=value1");
      sender.init();

      final Message request = new Message();
      request.setHeader("CamelHttpMethod", "POST");
      request.setPayload("Hello from Camel");

      final Message response = (Message) sender.send(request, null);

      server.close();

      Assert.assertEquals(response.getPayload(), TEST_RESPONSE);
      Assert.assertEquals(response.getHeader("CamelHttpResponseCode"), "201");
   }
}