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
    * Port where to receive responses.
    */
   private int port = 8088;

   /**
    * Host to bind to.
    */
   private String host = null;

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
      VertxOptions vertxOptions = new VertxOptions().setWorkerPoolSize(threads);
      Vertx vertx = Vertx.vertx(vertxOptions);
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

      if (host == null) {
         new Thread(() -> server.requestHandler(router::accept).listen(port)).start();
      } else {
         new Thread(() -> server.requestHandler(router::accept).listen(port, host)).start();
      }
   }

   @Override
   public void stop() {
      server.close();
   }

   /**
    * Gets the port number where to listen for response messages.
    *
    * @return The port number where to listen for response messages.
    */
   public int getPort() {
      return port;
   }

   /**
    * Sets the port number where to listen for response messages.
    *
    * @param port
    *       The port number where to listen for response messages.
    */
   public void setPort(final int port) {
      this.port = port;
   }

   /**
    * Gets the host where to listen for response messages. Null value means 0.0.0.0.
    *
    * @return The host where to listen for response messages.
    */
   public String getHost() {
      return host;
   }

   /**
    * Sets the host where to listen for response messages. Null value means 0.0.0.0.
    *
    * @param host
    *       The host where to listen for response messages.
    */
   public void setHost(final String host) {
      this.host = host;
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
    */
   public void setHttpStatusCode(final int httpStatusCode) {
      this.httpStatusCode = httpStatusCode;
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
    */
   public void setHttpStatusMessage(final String httpStatusMessage) {
      this.httpStatusMessage = httpStatusMessage;
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
    */
   public void setHttpResponse(final String httpResponse) {
      this.httpResponse = httpResponse;
   }
}
