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

import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

/**
 * This sender is intended to work as a dummy sender and to be used for
 * scenario testing and developing purposes. It does not actually send any message.
 * It can simulate a synchronous waiting for a reply by setting the {@link #delay} property in milliseconds (with default values 0).
 * property.
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class DummySender extends AbstractSender {
   /**
    * The sender's logger.
    */
   private static final Logger log = Logger.getLogger(DummySender.class);

   /**
    * The delay duration to simulate a asonchronous waiting.
    */
   private long delay = 0;

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.AbstractSender#init()
    */
   @Override
   public void init() throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Initializing... " + target);
      }
      // nop
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.AbstractSender#close()
    */
   @Override
   public void close() {
      if (log.isDebugEnabled()) {
         log.debug("Closing...");
      }
      // nop
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Sending to " + target + "...");
      }
      if (delay > 0) {
         Thread.sleep(delay);
      }
      // nop
      return (message == null) ? message : message.getPayload();
   }

   /**
    * Used to read the value of delay.
    *
    * @return The delay.
    */
   public long getDelay() {
      return delay;
   }

   /**
    * Sets the value of delay.
    *
    * @param delay
    *           The delay to set.
    */
   public DummySender setDelay(final long delay) {
      this.delay = delay;
      return this;
   }

}
