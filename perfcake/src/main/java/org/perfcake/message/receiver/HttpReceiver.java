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
package org.perfcake.message.receiver;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Receives responses using HTTP.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class HttpReceiver extends AbstractReceiver {

   /**
    * Cached HTTP server so it can be stopped.
    */
   private HttpServer server;

   /**
    * Handle to terminate Vertx.
    */
   private Vertx vertx;

   /**
    * HTTP status code to return to the client. Defaults to 200.
    */
   private int httpStatusCode = 200;

   /**
    * HTTP status message to return to the client. Null means it won't be set.
    */
   private String httpStatusMessage = null;

   /**
    * HTTP response to return to the client. Null means no response will be returned.
    */
   private String httpResponse = null;

   @Override
   public void start() throws PerfCakeException {
      String[] urlParts;
      if (getSource().length() == 0) {
         urlParts = new String[] { "8088" };
      } else {
         urlParts = getSource().split(":");
      }

      if (urlParts.length < 1 || urlParts.length > 2) {
         throw new PerfCakeException("Source has wrong format, expected [<host>:]<port> but found " + getSource());
      }

      VertxOptions vertxOptions = new VertxOptions().setWorkerPoolSize(threads);
      vertx = Vertx.vertx(vertxOptions);
      server = vertx.createHttpServer();
      Router router = Router.router(vertx);
      router.route().handler(BodyHandler.create());
      router.route().blockingHandler(context -> {
         correlator.registerResponse(context.getBodyAsString(), context.request().headers());
         final HttpServerResponse response = context.response();
         response.setStatusCode(httpStatusCode);

         if (httpStatusMessage != null) {
            response.setStatusMessage(httpStatusMessage);
         }

         if (httpResponse != null) {
            response.end(httpResponse);
         } else {
            response.end();
         }
      });

      if (urlParts.length == 1) {
         final Thread t = new Thread(() -> server.requestHandler(router::accept).listen(Integer.valueOf(urlParts[0])));
         t.setDaemon(true);
         t.start();
      } else {
         final Thread t = new Thread(() -> server.requestHandler(router::accept).listen(Integer.valueOf(urlParts[1]), urlParts[0]));
         t.setDaemon(true);
         t.start();
      }

      try {
         Thread.sleep(Integer.getInteger(PerfCakeConst.RECEIVER_BOOT_DELAY_PROPERTY, 500)); // make sure the listener gets started
      } catch (InterruptedException e) {
         // nps
      }
   }

   @Override
   public void stop() {
      server.close();
      vertx.close();
   }

   /**
    * Gets the HTTP status code to return to the client. Defaults to 200.
    *
    * @return The HTTP status code to return to the client.
    */
   public int getHttpStatusCode() {
      return httpStatusCode;
   }

   /**
    * Sets the HTTP status code to return to the client. Defaults to 200.
    *
    * @param httpStatusCode
    *       The HTTP status code to return to the client.
    * @return Instance of this to support fluent API.
    */
   public HttpReceiver setHttpStatusCode(final int httpStatusCode) {
      this.httpStatusCode = httpStatusCode;
      return this;
   }

   /**
    * Gets the HTTP status message to return to the client. Null means it won't be set.
    *
    * @return The HTTP status message to return to the client.
    */
   public String getHttpStatusMessage() {
      return httpStatusMessage;
   }

   /**
    * Sets the HTTP status message to return to the client. Null means it won't be set.
    *
    * @param httpStatusMessage
    *       The HTTP status message to return to the client.
    * @return Instance of this to support fluent API.
    */
   public HttpReceiver setHttpStatusMessage(final String httpStatusMessage) {
      this.httpStatusMessage = httpStatusMessage;
      return this;
   }

   /**
    * Gets the HTTP response to return to the client. Null means no response will be returned.
    *
    * @return The HTTP response to return to the client.
    */
   public String getHttpResponse() {
      return httpResponse;
   }

   /**
    * Sets the HTTP response to return to the client. Null means no response will be returned.
    *
    * @param httpResponse
    *       The HTTP response to return to the client.
    * @return Instance of this to support fluent API.
    */
   public HttpReceiver setHttpResponse(final String httpResponse) {
      this.httpResponse = httpResponse;
      return this;
   }
}
