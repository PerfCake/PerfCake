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
import java.util.Properties;

/**
 * Serves as a dummy sender to start developing a new sender. It does not actually send any message.
 * It can simulate a synchronous waiting for a reply by setting the {@link #delay} property in milliseconds (with default values 0).
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DummySender extends AbstractSender {
   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(DummySender.class);

   /**
    * The delay duration to simulate a asynchronous waiting.
    */
   private long delay = 0;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      final String currentTarget = safeGetTarget(messageAttributes);

      if (log.isDebugEnabled()) {
         log.debug("Initializing... " + currentTarget);
      }
   }

   @Override
   public void doClose() throws PerfCakeException {
      if (log.isDebugEnabled()) {
         log.debug("Closing Dummy sender.");
      }
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);
      if (log.isDebugEnabled()) {
         log.debug("Sending to " + safeGetTarget(messageAttributes) + "...");
      }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      if (delay > 0) {
         final long sleepStart = System.currentTimeMillis();
         try {
            Thread.sleep(delay);
         } catch (final InterruptedException ie) { // Snooze
            final long snooze = delay - (System.currentTimeMillis() - sleepStart);
            if (snooze > 0) {
               Thread.sleep(snooze);
            }
         }
      }

      return (message == null) ? null : message.getPayload();
   }

   /**
    * Gets read the value of delay.
    *
    * @return The delay in milliseconds.
    */
   public long getDelay() {
      return delay;
   }

   /**
    * Sets the value of delay.
    *
    * @param delay
    *       The delay to set in milliseconds.
    * @return Instance of this for fluent API.
    */
   public DummySender setDelay(final long delay) {
      this.delay = delay;
      return this;
   }
}
