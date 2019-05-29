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
package org.perfcake.message;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.message.correlator.GenerateHeaderCorrelator;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "integration" })
public class CorrelationIntegrationTest extends TestSetup {

   // HttpSender sends a message to localhost:8091, we pass it through to localhost:8092 and the receiver should get it
   @Test
   public void httpLoopTest() throws PerfCakeException {
      final Vertx vertx = Vertx.vertx();
      final HttpServer server = vertx.createHttpServer();
      final Router router = Router.router(vertx);
      final Set<Integer> messageNumbers = new ConcurrentHashSet<>();
      final HttpClient client = vertx.createHttpClient();
      router.route("/*").handler(BodyHandler.create());
      router.route("/*").handler((context) -> {
         messageNumbers.add(Integer.valueOf(context.getBodyAsString().split(" ")[2]));
         context.response().setStatusCode(200).end();

         client.post(8092, "localhost", "/", responseHandler -> {
         })
               .putHeader(GenerateHeaderCorrelator.CORRELATION_HEADER, context.request().getHeader(GenerateHeaderCorrelator.CORRELATION_HEADER))
               .end(context.getBodyAsString());
      });
      new Thread(() -> server.requestHandler(router::accept).listen(8091)).start();

      runScenario("test-correlation");

      server.close();

      // we should have seen each message number exactly once, so there must be 1000 of them
      Assert.assertEquals(messageNumbers.size(), 1000);
   }
}