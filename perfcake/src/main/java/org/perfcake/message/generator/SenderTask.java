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
package org.perfcake.message.generator;

import org.perfcake.PerfCakeConst;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.ReceivedMessage;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.message.sequences.SequenceManager;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.ValidationManager;
import org.perfcake.validation.ValidationTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;

/**
 * Executes a single task of sending messages from the message store
 * using instances of {@link MessageSender} provided by message sender manager (see {@link org.perfcake.message.sender.MessageSenderManager}),
 * receiving the message sender's response and handling the reporting and response message validation.
 * Sender task is not part of the public API, it is used from generators.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @see org.perfcake.message.sender.MessageSenderManager
 */
class SenderTask implements Runnable {

   /**
    * Sender task's logger.
    */
   private final Logger log = LogManager.getLogger(SenderTask.class);

   /**
    * Reference to a message sender manager that is providing the message senders.
    */
   private MessageSenderManager senderManager;

   /**
    * Reference to a message store where the messages are taken from.
    */
   private List<MessageTemplate> messageStore;

   /**
    * Indicates whether the message numbering is enabled or disabled.
    */
   private boolean messageNumberingEnabled;

   /**
    * Reference to a report manager.
    */
   private ReportManager reportManager;

   /**
    * A reference to the current validator manager. It is used to validate message responses.
    */
   private ValidationManager validationManager;

   /**
    * Instance dependant message attributes of this sender tasks. These are filled in by the generator from a {@link SequenceManager}.
    */
   private Properties messageAttributes;

   /**
    * Controls the amount of prepared tasks in a buffer.
    */
   private final Semaphore semaphore;

   /**
    * Creates a new task to send a message.
    * The semaphore is released when the task is finished. This is used to control the maximum number sender tasks created and waiting for execution.
    * The visibility of this constructor is limited as it is not intended for normal use.
    * To obtain a new instance of a sender task properly initialized call
    * {@link org.perfcake.message.generator.AbstractMessageGenerator#newSenderTask(java.util.concurrent.Semaphore)}.
    *
    * @param semaphore
    *       A semaphore to be released once the task is finished.
    */
   protected SenderTask(final Semaphore semaphore) {
      this.semaphore = semaphore;
   }

   private Serializable sendMessage(final MessageSender sender, final Message message, final HashMap<String, String> messageHeaders, final MeasurementUnit mu) {
      try {
         sender.preSend(message, messageHeaders);
      } catch (final Exception e) {
         if (log.isErrorEnabled()) {
            log.error("Exception occurred!", e);
         }
      }

      mu.startMeasure();

      Serializable result = null;
      try {
         result = sender.send(message, messageHeaders, mu);
      } catch (final Exception e) {
         if (log.isErrorEnabled()) {
            log.error("Exception occurred!", e);
         }
      }
      mu.stopMeasure();

      try {
         sender.postSend(message);
      } catch (final Exception e) {
         if (log.isErrorEnabled()) {
            log.error("Exception occurred!", e);
         }
      }

      return result;
   }

   /**
    * Executes the scheduled sender task. This is supposed to be controlled by an enclosing thread.
    */
   @Override
   public void run() {
      assert messageStore != null && reportManager != null && validationManager != null && senderManager != null : "SenderTask was not properly initialized.";

      final HashMap<String, String> messageHeaders = new HashMap<>();
      MessageSender sender = null;
      ReceivedMessage receivedMessage = null;
      try {
         final MeasurementUnit mu = reportManager.newMeasurementUnit();

         if (mu != null) {
            // only set numbering to headers if it is enabled, later there is no change to
            // filter out the headers before sending
            if (messageNumberingEnabled && messageAttributes != null) {
               messageHeaders.put(PerfCakeConst.MESSAGE_NUMBER_HEADER, messageAttributes.getProperty(PerfCakeConst.MESSAGE_NUMBER_PROPERTY, String.valueOf(mu.getIteration())));
            }

            sender = senderManager.acquireSender();

            final Iterator<MessageTemplate> iterator = messageStore.iterator();
            if (iterator.hasNext()) {
               while (iterator.hasNext()) {

                  final MessageTemplate messageToSend = iterator.next();
                  final Message currentMessage = messageToSend.getFilteredMessage(messageAttributes);
                  final long multiplicity = messageToSend.getMultiplicity();

                  for (int i = 0; i < multiplicity; i++) {
                     receivedMessage = new ReceivedMessage(sendMessage(sender, currentMessage, messageHeaders, mu), messageToSend, currentMessage, messageAttributes);
                     if (validationManager.isEnabled()) {
                        validationManager.submitValidationTask(new ValidationTask(Thread.currentThread().getName(), receivedMessage));
                     }
                  }

               }
            } else {
               receivedMessage = new ReceivedMessage(sendMessage(sender, null, messageHeaders, mu), null, null, messageAttributes);
               if (validationManager.isEnabled()) {
                  validationManager.submitValidationTask(new ValidationTask(Thread.currentThread().getName(), receivedMessage));
               }
            }

            senderManager.releaseSender(sender); // !!! important !!!
            sender = null;

            reportManager.report(mu);
         }
      } catch (final Exception e) {
         e.printStackTrace();
      } finally {
         if (semaphore != null) {
            semaphore.release();
         }

         if (sender != null) {
            senderManager.releaseSender(sender);
         }
      }
   }

   /**
    * Configures a {@link org.perfcake.message.sender.MessageSenderManager} for the sender task.
    *
    * @param senderManager
    *       {@link org.perfcake.message.sender.MessageSenderManager} to be used by the sender task.
    */
   protected void setSenderManager(final MessageSenderManager senderManager) {
      this.senderManager = senderManager;
   }

   /**
    * Configures a message store for the sender task.
    *
    * @param messageStore
    *       Message store to be used by the sender task.
    */
   protected void setMessageStore(final List<MessageTemplate> messageStore) {
      this.messageStore = messageStore;
   }

   /**
    * Enables or disables marking the messages with a unique number. Disable this for maximal performance.
    *
    * @param messageNumberingEnabled
    *       True to enable message numbering, false otherwise.
    */
   protected void setMessageNumberingEnabled(final boolean messageNumberingEnabled) {
      this.messageNumberingEnabled = messageNumberingEnabled;
   }

   /**
    * Configures a {@link org.perfcake.reporting.ReportManager} for the sender task.
    *
    * @param reportManager
    *       {@link org.perfcake.reporting.ReportManager} to be used by the sender task.
    */
   protected void setReportManager(final ReportManager reportManager) {
      this.reportManager = reportManager;
   }

   /**
    * Configures a {@link org.perfcake.validation.ValidationManager} for the sender task.
    *
    * @param validationManager
    *       {@link org.perfcake.validation.ValidationManager} to be used by the sender task.
    */
   protected void setValidationManager(final ValidationManager validationManager) {
      this.validationManager = validationManager;
   }

   /**
    * Sets instance dependant message attributes of this sender task. This should be done by a generator using {@link SequenceManager}.
    * @param messageAttributes Current message attributes obtained from a {@link SequenceManager}.
    */
   public void setMessageAttributes(final Properties messageAttributes) {
      this.messageAttributes = messageAttributes;
   }
}
