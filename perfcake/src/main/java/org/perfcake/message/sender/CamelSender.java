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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

/**
 * Sends requests to a Camel endpoint which extends the variety of protocols PerfCake can support.
 * Do not forget to add Camel components on the classpath (in ext directory).
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CamelSender extends AbstractSender {

   /**
    * The Camel context.
    */
   private CamelContext context;

   /**
    * Message producer.
    */
   private ProducerTemplate template;

   /**
    * Cached endpoint string from target with replaced placeholders.
    */
   private String endpoint;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      try {
         context = new DefaultCamelContext();
         context.start();
         template = context.createProducerTemplate();
         endpoint = safeGetTarget(messageAttributes);
      } catch (Exception e) {
         throw new PerfCakeException("Unable to initialize Camel: ", e);
      }
   }

   @Override
   public void doClose() throws PerfCakeException {
      try {
         context.stop();
      } catch (Exception e) {
         throw new PerfCakeException("Unable to terminate Camel: ", e);
      }
   }

   @Override
   public Serializable doSend(final Message message, final MeasurementUnit measurementUnit) throws Exception {
      final Exchange exchange = template.send(endpoint, new MessageProcessor(message.getPayload(), message.getHeaders()));

      final Message response = new Message();
      response.setPayload(exchange.getOut().getBody(String.class));
      exchange.getOut().getHeaders().forEach((k, v) -> response.setHeader(k, v.toString()));

      return response;
   }

   /**
    * Sender of outbound messages (including their headers).
    */
   private static class MessageProcessor implements Processor {

      private final Object body;
      private final Map<String, Object> headers;

      @SuppressWarnings("unchecked")
      MessageProcessor(final Object body, final Properties headers) {
         this.body = body;
         this.headers = (Map) headers; // quick and dirty hack
      }

      @Override
      public void process(Exchange exchange) throws Exception {
         exchange.getIn().setBody(body);
         exchange.getIn().setHeaders(headers);
      }
   }
}
