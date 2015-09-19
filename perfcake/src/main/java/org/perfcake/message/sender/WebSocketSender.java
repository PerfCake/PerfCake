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
import org.perfcake.reporting.MeasurementUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * Sends a simple messages via websocket protocol to a remote websocket server endpoint.
 *
 * @author Jiří Sviták
 */
public class WebSocketSender extends AbstractSender {

   private static final Logger logger = LogManager.getLogger(WebSocketSender.class);

   private WebSocketContainer container;
   private Session session;

   /**
    * Remote endpoint type.
    *
    * @author Jiří Sviták
    */
   public enum RemoteEndpointType {
      BASIC, ASYNC
   }

   private RemoteEndpointType remoteEndpointType = RemoteEndpointType.BASIC;

   /**
    * Payload type.
    *
    * @author Jiří Sviták
    */
   public enum PayloadType {
      TEXT, BINARY, PING
   }

   private PayloadType payloadType = PayloadType.TEXT;

   /**
    * Sets remote endpoint type.
    *
    * @param remoteEndpointType
    *       The remote endpoint type. The value should be one of <code>basic</code> or <code>async</code>.
    * @return Instance of this to support fluent API.
    */
   public WebSocketSender setRemoteEndpointType(final String remoteEndpointType) {
      switch (remoteEndpointType) {
         case "basic":
            this.remoteEndpointType = RemoteEndpointType.BASIC;
            break;
         case "async":
            this.remoteEndpointType = RemoteEndpointType.ASYNC;
            break;
         default:
            throw new IllegalStateException("Unknown or undefined web socket remote endpoint type. Use either basic or async.");
      }

      return this;
   }

   /**
    * Sets payload type.
    *
    * @param payloadType
    *       The remote endpoint type. The value should be one of <code>text</code>, <code>binary</code> or <code>ping</code>.
    * @return Instance of this to support fluent API.
    */
   public WebSocketSender setPayloadType(final String payloadType) {
      switch (payloadType) {
         case "text":
            this.payloadType = PayloadType.TEXT;
            break;
         case "binary":
            this.payloadType = PayloadType.BINARY;
            break;
         case "ping":
            this.payloadType = PayloadType.PING;
            break;
         default:
            throw new IllegalStateException("Unknown or undefined web socket payload type. Use text, binary, or ping.");
      }

      return this;
   }

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      container = ContainerProvider.getWebSocketContainer();
      try {
         final String safeTarget = safeGetTarget(messageAttributes);

         if (logger.isTraceEnabled()) {
            logger.trace("Connecting to URI " + safeTarget);
         }

         container.connectToServer(new PerfCakeClientEndpoint(), new URI(safeTarget));
      } catch (IOException | DeploymentException | URISyntaxException e) {
         throw new PerfCakeException("Cannot open web socket: ", e);
      }
      if (session == null) {
         throw new PerfCakeException("Web socket session cannot be null before the scenario run.");
      }
   }

   @Override
   public void doClose() throws PerfCakeException {
      try {
         session.close();
      } catch (final IOException e) {
         throw new PerfCakeException("Cannot close web socket session.", e);
      }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      if (remoteEndpointType == RemoteEndpointType.BASIC) {
         final RemoteEndpoint.Basic basic = session.getBasicRemote();
         switch (payloadType) {
            case TEXT:
               basic.sendText(message.getPayload().toString());
               break;
            case BINARY:
               throw new UnsupportedOperationException("Web socket binary payload is not supported yet.");
               // basic.sendBinary(null);
            case PING:
               throw new UnsupportedOperationException("Web socket ping payload is not supported yet.");
               // basic.sendPing(null);
            default:
               throw new IllegalStateException("Unknown or undefined web socket payload type. Use text, binary or ping.");
         }
      } else if (remoteEndpointType == RemoteEndpointType.ASYNC) {
         final RemoteEndpoint.Async async = session.getAsyncRemote();
         switch (payloadType) {
            case TEXT:
               async.sendText(message.getPayload().toString());
               break;
            case BINARY:
               throw new UnsupportedOperationException("Web socket binary payload is not supported yet.");
               // async.sendBinary(null);
            case PING:
               throw new UnsupportedOperationException("Web socket ping payload is not supported yet.");
               // async.sendPing(null);
            default:
               throw new IllegalStateException("Unknown or undefined web socket payload type. Use text, binary or ping.");
         }
      } else {
         throw new IllegalStateException("Unknown or undefined web socket remote endpoint type. Use either basic or async.");
      }
      return null;
   }

   /**
    * Represents web socket client endpoint.
    *
    * @author Jiří Sviták
    */
   @ClientEndpoint
   public class PerfCakeClientEndpoint {

      /**
       * Is called when a new web socket session is open.
       *
       * @param session
       *       Web socket session.
       */
      @OnOpen
      public void onOpen(final Session session) {
         if (logger.isTraceEnabled()) {
            logger.trace("Connected with session id: " + session.getId());
         }
         WebSocketSender.this.session = session;
      }

      /**
       * Receives incoming web socket messages.
       *
       * @param message
       *       Incomming message.
       * @param session
       *       Web socket session.
       */
      @OnMessage
      public void onMessage(final String message, final Session session) {
         if (logger.isTraceEnabled()) {
            logger.trace("Received message: " + message);
         }
      }

      /**
       * Is called when a web socket session is closing.
       *
       * @param session
       *       Web socket session.
       * @param closeReason
       *       The reason why a web socket has been closed, or why it is being asked to close
       */
      @OnClose
      public void onClose(final Session session, final CloseReason closeReason) {
         if (logger.isTraceEnabled()) {
            logger.trace(String.format("Session %s closed because of %s", session.getId(), closeReason));
         }
      }
   }
}
