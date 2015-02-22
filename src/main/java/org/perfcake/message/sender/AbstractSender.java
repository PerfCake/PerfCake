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
 * The common ancestor for all senders.
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

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.MessageSender#init()
    */
   @Override
   abstract public void init() throws Exception;

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.MessageSender#close()
    */
   @Override
   abstract public void close() throws PerfCakeException;

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.MessageSender#send(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public final Serializable send(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      return doSend(message, properties, mu);
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.MessageSender#preSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public void preSend(final Message message, final Map<String, String> properties) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Initializing sending of a message.");
         if (log.isTraceEnabled()) {
            log.trace(String.format("Message content: %s", message.toString()));
         }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.MessageSender#doSend(org.perfcake.message.Message, org.perfcake.reporting.MeasurementUnit)
    */
   final public Serializable doSend(final Message message, final MeasurementUnit mu) throws Exception {
      return this.doSend(message, null, mu);
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.MessageSender#doSend(org.perfcake.message.Message, java.util.Properties, org.perfcake.reporting.MeasurementUnit)
    */
   abstract public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception;

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.MessageSender#postSend(org.perfcake.message.Message)
    */
   @Override
   public void postSend(final Message message) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Cleaning up after the message has been sent.");
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.MessageSender#send(org.perfcake.message.Message)
    */
   @Override
   public final Serializable send(final Message message, final MeasurementUnit mu) throws Exception {
      return send(message, null, mu);
   }

   /**
    * Read the target where to send the messages.
    *
    * @return The current target.
    */
   public String getTarget() {
      return target;
   }

   /**
    * Sets the target where to send the messages.
    *
    * @param target
    *       The target to be set.
    * @return Instance of this for fluent API.
    */
   public AbstractSender setTarget(final String target) {
      this.target = target;
      return this;
   }
}
