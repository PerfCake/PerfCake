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
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.io.Serializable;
import java.util.Properties;

/**
 * CoAP sender.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class CoapSender extends AbstractSender {

   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(CoapSender.class);

   /**
    * CoAP Java API client.
    */
   private CoapClient client;

   /**
    * CoAP response.
    */
   private CoapResponse coapResponse;

   /**
    * CoAP request method.
    */
   private enum CoapMethod {
      GET, POST, PUT, DELETE
   }

   /**
    * CoAP request type.
    */
   private enum CoapRequestType {
      /**
       * Non-confirmable request.
       */
      NON_CONFIRMABLE,

      /**
       * Confirmable request.
       */
      CONFIRMABLE
   }

   // Properties
   /**
    * CoAP request type that the sender will use.
    */
   private CoapRequestType requestType = CoapRequestType.NON_CONFIRMABLE;

   /**
    * CoAP request method that the sender will use.
    */
   private CoapMethod method = CoapMethod.POST;

   @Override
   public void doInit(Properties messageAttributes) throws PerfCakeException {
      client = new CoapClient(safeGetTarget(messageAttributes));
      switch (requestType) {
         case CONFIRMABLE:
            client.useCONs();
            break;
         case NON_CONFIRMABLE:
            client.useNONs();
            break;
      }
   }

   @Override
   public void preSend(final Message message, final Properties messageAttributes) throws Exception {
      super.preSend(message, messageAttributes);
   }

   @Override
   public Serializable doSend(Message message, MeasurementUnit measurementUnit) throws Exception {
      switch (method) {
         case GET:
            coapResponse = client.get();
            break;
         case POST:
            coapResponse = client.post(message.getPayload().toString(), MediaTypeRegistry.TEXT_PLAIN);
            break;
         case PUT:
            coapResponse = client.put(message.getPayload().toString(), MediaTypeRegistry.TEXT_PLAIN);
            break;
         case DELETE:
            coapResponse = client.delete();
            break;
      }
      return coapResponse.getResponseText();
   }

   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);
      //nop
   }

   @Override
   public void doClose() throws PerfCakeException {
      // nop
   }

   /**
    * Returns the method that the CoAP will use to send the requests.
    *
    * @return The method's name.
    */
   public CoapMethod getMethod() {
      return method;
   }

   /**
    * Sets the CoAP method.        One of GET, POST, DELETE and PUT is supported.
    *
    * @param method
    *       The CoAP request method.
    * @return The instance of this for a fluent API.
    */
   public CoapSender setMethod(final CoapMethod method) {
      this.method = method;
      return this;
   }

   /**
    * Returns the method that the CoAP will use to send the requests.
    *
    * @return The method's name.
    */
   public CoapRequestType getRequestType() {
      return requestType;
   }

   /**
    * Sets the CoAP request type. Confirmable or Non-confirmable requests are supported.
    *
    * @param requestType
    *       The type of CoAP request.
    * @return The instance of this for a fluent API.
    */
   public CoapSender setRequestType(final CoapRequestType requestType) {
      this.requestType = requestType;
      return this;
   }
}
