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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Serves as a dummy sender for scenario testing and developing purposes. It does not actually send any message.
 * It can simulate a synchronous waiting for a reply by setting the {@link #delay} property in milliseconds (with default values 0).
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class TestSender extends AbstractSender {
   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(TestSender.class);

   /**
    * Iteration counter (how many times the doSend method has been called).
    */
   private static AtomicLong counter = new AtomicLong(0);

   /**
    * The delay duration to simulate a asynchronous waiting.
    */
   private long delay = 0;

   /**
    * Can switch on recording of each message payload sent.
    */
   private boolean recording = false;

   /**
    * Contains all recorded messages in case the {@link #recording} was switched on.
    */
   private static List<String> recordedMessages = Collections.synchronizedList(new ArrayList<>());

   /**
    * A listener where the computed target for each initialization is reported.
    */
   private static Consumer<String> onInitListener = null;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      final String currentTarget = safeGetTarget(messageAttributes);

      if (log.isDebugEnabled()) {
         log.debug("Initializing... " + currentTarget);
      }

      if (onInitListener != null) {
         onInitListener.accept(currentTarget);
      }
   }

   /**
    * Sets a listener where current computed target address from calls to {@link #doInit(Properties)} are reported.
    * This method is not thread-safe, you can use it only before the scenario is started, or after it is finished.
    * Do not forget to cleanup your listener by passing null argument.
    *
    * @param listener
    *       The listener where to report the target.
    */
   public static void setOnInitListener(final Consumer<String> listener) {
      onInitListener = listener;
   }

   @Override
   public void doClose() {
      if (log.isDebugEnabled()) {
         log.debug("Closing...");
      }
      // nop
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
      final long count = counter.incrementAndGet();

      if (log.isDebugEnabled()) {
         log.debug("Dummy counter: " + count);
      }

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

      if (recording && message != null) {
         recordedMessages.add(message.getPayload().toString());
      }

      if (message != null && message.toString().contains("fail me please")) {
         throw new IOException("Failing per user request.");
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
   public TestSender setDelay(final long delay) {
      this.delay = delay;
      return this;
   }

   /**
    * Obtains the current state of the recording.
    *
    * @return True iff recording of message payloads passed through this sender is switched on.
    */
   public boolean isRecording() {
      return recording;
   }

   /**
    * Switches on recording of message payloads passed through this sender.
    *
    * @param recording
    *       True iff the recording should be turned on.
    */
   public void setRecording(final boolean recording) {
      this.recording = recording;
   }

   /**
    * Gets the list of recorded message payloads passed through this message sender while recording was switched on.
    *
    * @return The list of recorded message payloads passed through this message sender while recording was switched on.
    */
   public static List<String> getRecordedMessages() {
      return recordedMessages;
   }

   /**
    * Clears all recorded messages.
    */
   public static void resetRecordings() {
      recordedMessages.clear();
   }

   /**
    * Resets the iteration counter (how many times the {@link #doSend(org.perfcake.message.Message, java.util.Map, org.perfcake.reporting.MeasurementUnit)} method has been called).
    */
   public static void resetCounter() {
      counter.set(0);
   }

   /**
    * Gets the iteration counter (how many times the {@link #doSend(org.perfcake.message.Message, java.util.Map, org.perfcake.reporting.MeasurementUnit)} method has been called).
    *
    * @return The iteration counter value.
    */
   public static long getCounter() {
      return counter.get();
   }
}
