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

import java.io.Serializable;
import java.util.Map;

/**
 * The common ancestor for all senders. Facilitates logging and target specification.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
abstract public class AbstractSender implements MessageSender {

   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(AbstractSender.class);

   /**
    * The target where to send the messages.
    */
   protected String target = "";

   @Override
   abstract public void init() throws Exception;

   @Override
   abstract public void close() throws PerfCakeException;

   @Override
   public final Serializable send(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      return doSend(message, properties, measurementUnit);
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Initializing sending of a message.");
         if (log.isTraceEnabled()) {
            log.trace(String.format("Message content: %s", message.toString()));
         }
      }
   }

   /**
    * Actually performs the send operation. Relies to {@link #doSend(org.perfcake.message.Message, java.util.Map, org.perfcake.reporting.MeasurementUnit)}.
    * Marked as final to prevent overriding. Override {@link #doSend(org.perfcake.message.Message, java.util.Map, org.perfcake.reporting.MeasurementUnit)} instead.
    *
    * @param message
    *       Message to be sent.
    * @param measurementUnit
    *       Measurement unit carrying the current stop-watch.
    * @return Response to the message.
    * @throws Exception
    *       When the sending operation failed.
    * @see org.perfcake.message.sender.MessageSender#send(org.perfcake.message.Message, org.perfcake.reporting.MeasurementUnit)
    */
   final public Serializable doSend(final Message message, final MeasurementUnit measurementUnit) throws Exception {
      return this.doSend(message, null, measurementUnit);
   }

   /**
    * Actually performs the send operation. Should be overridden by specific implementations.
    *
    * @param message
    *       Message to be sent.
    * @param properties
    *       Additional properties that can influence the sending of the message.
    * @param measurementUnit
    *       Measurement unit carrying the current stop-watch.
    * @return Response to the message.
    * @throws Exception
    *       When the sending operation failed.
    * @see org.perfcake.message.sender.MessageSender#send(org.perfcake.message.Message, java.util.Map, org.perfcake.reporting.MeasurementUnit)
    */
   abstract public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception;

   @Override
   public void postSend(final Message message) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Cleaning up after the message has been sent.");
      }
   }

   @Override
   public final Serializable send(final Message message, final MeasurementUnit measurementUnit) throws Exception {
      return send(message, null, measurementUnit);
   }

   @Override
   public String getTarget() {
      return target;
   }

   @Override
   public AbstractSender setTarget(final String target) {
      this.target = target;
      return this;
   }
}
