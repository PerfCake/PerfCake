/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2017 the original author or authors.
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

import org.perfcake.TestSetup;
import org.perfcake.message.Message;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class OauthHttpSenderTest extends TestSetup {

   public static final String TOKEN = "abcdef0123456789";
   public static final String RESPONSE = "You made it through!";

   @Test
   public void tokenTest() throws Exception {
      final Vertx vertx = Vertx.vertx();
      final HttpServer server = vertx.createHttpServer();
      final Router router = Router.router(vertx);
      final LongAdder counter = new LongAdder();
      router.route("/*").handler(BodyHandler.create());
      router.post("/token").handler(context -> {
         if ("gimme".equals(context.getBodyAsString())) {
            counter.increment();
            context.response().end("{\"access_token\":\"" + TOKEN + counter.longValue() + "\",\"expires\":\"2736817\"}");
         } else {
            context.response().end();
         }
      });
      router.get("/app").handler(context -> {
         final String token = context.request().getHeader("Authorization");
         if (token != null && token.equals("Bearer " + TOKEN + counter.longValue())) {
            context.response().end(RESPONSE);
         } else {
            context.response().end();
         }
      });

      new Thread(() -> server.requestHandler(router::accept).listen(8095)).start();

      Thread.sleep(500);

      final OauthHttpSender s = new OauthHttpSender();
      final Message m = new Message();

      s.setTokenServerUrl("http://127.0.0.1:8095/token");
      s.setTokenServerData("gimme");
      s.setTarget("http://localhost:8095/app");
      s.setMethod(HttpSender.Method.GET);
      s.setTokenTimeout(100);
      s.init();

      s.preSend(m, null);
      final String response = s.send(m, null).toString();
      s.postSend(m);

      s.preSend(m, null);
      s.send(m, null);
      s.postSend(m);

      Thread.sleep(150); // let's wait for the token timeout

      s.preSend(m, null);
      final String response2 = s.send(m, null).toString();
      s.postSend(m);

      s.close();

      server.close();

      Assert.assertEquals(response, RESPONSE);
      Assert.assertEquals(response2, RESPONSE);
      Assert.assertEquals(counter.longValue(), 2);
   }

}