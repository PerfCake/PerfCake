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
import org.perfcake.util.StringTemplate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Properties;

/**
 * The common ancestor for all senders. Facilitates logging and target specification.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public abstract class AbstractSender implements MessageSender {

   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(AbstractSender.class);

   /**
    * We need to cache the value to be really fast.
    */
   private boolean isTraceEnabled = false;

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
      isTraceEnabled = log.isTraceEnabled();
   }

   public abstract void doInit(final Properties messageAttributes) throws PerfCakeException;

   @Override
   public final void close() throws PerfCakeException {
      if (keepConnection) { // otherwise this is done in postSend()
         doClose();
      }
   }

   public abstract void doClose() throws PerfCakeException;

   @Override
   public final Serializable send(final Message message, final MeasurementUnit measurementUnit) throws Exception {
      return doSend(message, measurementUnit);
   }

   @Override
   public void preSend(final Message message, final Properties messageAttributes) throws Exception {
      if (isTraceEnabled) {
         log.trace(String.format("Message content: %s", (message == null) ? null : message.toString()));
      }

      if (!keepConnection) {
         doInit(messageAttributes);
      }
   }

   /**
    * Actually performs the send operation. Should be overridden by specific implementations.
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
   public abstract Serializable doSend(final Message message, final MeasurementUnit measurementUnit) throws Exception;

   @Override
   public void postSend(final Message message) throws Exception {
      if (!keepConnection) {
         doClose();
      }
   }

   @Override
   public final String getTarget() {
      return target.toString();
   }

   @Override
   public final String getTarget(final Properties properties) {
      return target.toString(properties);
   }

   /**
    * Gets the target in a safe way to avoid NPE when properties are null.
    *
    * @param properties
    *       Properties to replace placeholders in the target.
    * @return The target template with placeholders replaced.
    */
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
    * @return Instance of this to support fluent API.
    */
   public AbstractSender setKeepConnection(final boolean keepConnection) {
      this.keepConnection = keepConnection;
      return this;
   }
}
