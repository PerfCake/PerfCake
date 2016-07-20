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

   private CoapClient client;
   private CoapResponse coapResponse;

   private enum CoapMethod {
      GET, POST, PUT, DELETE
   }

   private enum CoapRequestType {
      NON, CON
   }

   // Properties
   private CoapRequestType requestType = CoapRequestType.NON;
   private CoapMethod method = CoapMethod.POST;

   @Override
   public void doInit(Properties messageAttributes) throws PerfCakeException {
      client = new CoapClient(safeGetTarget(messageAttributes));
      switch (requestType) {
         case CON:
            client.useCONs();
            break;
         case NON:
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

   public CoapMethod getMethod() {
      return method;
   }

   public CoapSender setMethod(final CoapMethod method) {
      this.method = method;
      return this;
   }

   public CoapRequestType getRequestType() {
      return requestType;
   }

   public CoapSender setRequestType(final CoapRequestType requestType) {
      this.requestType = requestType;
      return this;
   }
}
