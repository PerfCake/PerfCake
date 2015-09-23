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
import org.perfcake.util.StringTemplate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

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
   private StringTemplate target = new StringTemplate("");

   /**
    * Keeps the same connection to the target between individual invocations.
    * This can achieve higher throughput, however, target cannot be changed for individual
    * invocations and it does not make sense to use placeholders in target.
    */
   protected boolean keepConnection = true;

   @Override
   public final void init() throws PerfCakeException {
      if (keepConnection) { // otherwise this is done in preSend()
         doInit(null);
      }
   }

   abstract public void doInit(final Properties messageAttributes) throws PerfCakeException;

   @Override
   public final void close() throws PerfCakeException {
      if (keepConnection) { // otherwise this is done in postSend()
         doClose();
      }
   }

   abstract public void doClose() throws PerfCakeException;

   @Override
   public final Serializable send(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      return doSend(message, properties, measurementUnit);
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      if (log.isTraceEnabled()) {
         log.trace(String.format("Message content: %s", message.toString()));
      }

      if (!keepConnection) {
         doInit(messageAttributes);
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
   public final Serializable doSend(final Message message, final MeasurementUnit measurementUnit) throws Exception {
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
      if (!keepConnection) {
         doClose();
      }
   }

   @Override
   public final Serializable send(final Message message, final MeasurementUnit measurementUnit) throws Exception {
      return send(message, null, measurementUnit);
   }

   @Override
   public final String getTarget() {
      return target.toString();
   }

   public final String getTarget(final Properties properties) {
      return target.toString(properties);
   }

   public final String safeGetTarget(final Properties properties) {
      if (properties == null) {
         return getTarget();
      } else {
         return getTarget(properties);
      }
   }

   @Override
   public final AbstractSender setTarget(final String target) {
      this.target = new StringTemplate(target);
      return this;
   }

   /**
    * Should we try to preserve connection between sending of individual messages?
    * Placeholders in the target address cannot be replaced with properties nor sequences when set to true.
    * Defaults to true.
    *
    * @return True iff the connection is kept open.
    */
   public boolean isKeepConnection() {
      return keepConnection;
   }

   /**
    * Should we try to preserve connection between sending of individual messages?
    * Placeholders in the target address cannot be replaced with properties nor sequences when set to true.
    * Defaults to true.
    *
    * @param keepConnection
    *       True when the connection should be kept open.
    */
   public void setKeepConnection(final boolean keepConnection) {
      this.keepConnection = keepConnection;
   }
}
