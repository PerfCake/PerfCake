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
package org.perfcake.message.generator;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.ReceivedMessage;
import org.perfcake.message.correlator.Correlator;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.message.sequence.SequenceManager;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.ValidationManager;
import org.perfcake.validation.ValidationTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

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
public class SenderTask implements Runnable {

   /**
    * Sender task's logger.
    */
   private static final Logger log = LogManager.getLogger(SenderTask.class);

   /**
    * Limit the number of warning about interrupted response receival at the end of the test.
    */
   private static volatile AtomicInteger interruptWarningDisplayed = new AtomicInteger(0);

   /**
    * Reference to a message sender manager that is providing the message senders.
    */
   private MessageSenderManager senderManager;

   /**
    * Reference to a message store where the messages are taken from.
    */
   private List<MessageTemplate> messageStore;

   /**
    * Reference to a report manager.
    */
   private ReportManager reportManager;

   /**
    * A reference to the current validator manager. It is used to validate message responses.
    */
   private ValidationManager validationManager;

   /**
    * A reference to the current sequence manager. This is used for determining message specific sequence values.
    */
   private SequenceManager sequenceManager;

   /**
    * Message generator that created this sender task.
    */
   private final MessageGenerator messageGenerator;

   /**
    * The time when the task was enqueued.
    */
   private long enqueueTime = System.nanoTime();

   /**
    * Correlator to correlate message received from a separate channel.
    */
   private Correlator correlator = null;

   /**
    * A response matched by a correlator.
    */
   private Serializable correlatedResponse = null;

   /**
    * Synchronize on waiting for a message from a correlator.
    * Must be set before correlator is used.
    */
   private Semaphore waitForResponse;

   /**
    * Creates a new task to send a message.
    * There is a communication channel established that allows and requires the sender task to report the task completion and any possible error.
    * The visibility of this constructor is limited as it is not intended for normal use.
    * To obtain a new instance of a sender task properly initialized call
    * {@link org.perfcake.message.generator.AbstractMessageGenerator#newSenderTask(java.util.concurrent.Semaphore)}.
    *
    * @param messageGenerator
    *       The message generator that created this sender task.
    */
   protected SenderTask(final MessageGenerator messageGenerator) {
      this.messageGenerator = messageGenerator;
   }

   /**
    * Reports an exception from a sender when the generator is supposed to fail fast. This terminates test execution.
    *
    * @param e
    *       The error from the sender to be reported.
    */
   private void reportSenderError(final Exception e) {
      if (messageGenerator.isFailFast()) {
         messageGenerator.interrupt(e);
      }
   }

   private Serializable sendMessage(final MessageSender sender, final Message message, final Properties messageAttributes, final MeasurementUnit mu) {
      try {
         sender.preSend(message, messageAttributes);
      } catch (final Exception e) {
         if (log.isErrorEnabled()) {
            log.error("Unable to initialize sending of a message: ", e);
         }
         reportSenderError(e);
      }

      mu.startMeasure();

      Serializable result = null;
      try {
         result = sender.send(message, mu);
      } catch (final Exception e) {
         mu.setFailure(e);
         if (log.isErrorEnabled()) {
            log.error("Unable to send a message: ", e);
         }
         reportSenderError(e);
      }
      mu.stopMeasure();

      try {
         sender.postSend(message);
      } catch (final Exception e) {
         if (log.isErrorEnabled()) {
            log.error("Unable to finish sending of a message: ", e);
         }
         reportSenderError(e);
      }

      return result;
   }

   /**
    * Executes the scheduled sender task. This is supposed to be controlled by an enclosing thread.
    */
   @Override
   public void run() {
      assert messageStore != null && reportManager != null && validationManager != null && senderManager != null : "SenderTask was not properly initialized.";

      final Properties messageAttributes = sequenceManager != null ? sequenceManager.getSnapshot() : new Properties();

      MessageSender sender = null;
      ReceivedMessage receivedMessage;
      try {
         final MeasurementUnit mu = reportManager.newMeasurementUnit();
         long requestSize = 0;
         long responseSize = 0;

         if (mu != null) {
            mu.setEnqueueTime(enqueueTime);

            if (messageAttributes != null) {
               mu.appendResult(PerfCakeConst.ATTRIBUTES_TAG, messageAttributes);
               messageAttributes.put(PerfCakeConst.ITERATION_NUMBER_PROPERTY, String.valueOf(mu.getIteration()));
            }
            mu.appendResult(PerfCakeConst.THREADS_TAG, reportManager.getRunInfo().getThreads());

            sender = senderManager.acquireSender();

            final Iterator<MessageTemplate> iterator = messageStore.iterator();
            if (iterator.hasNext()) {
               while (iterator.hasNext()) {

                  final MessageTemplate messageToSend = iterator.next();
                  final Message currentMessage = messageToSend.getFilteredMessage(messageAttributes);
                  final long multiplicity = messageToSend.getMultiplicity();
                  requestSize = requestSize + (currentMessage.getPayload().toString().length() * multiplicity);

                  for (int i = 0; i < multiplicity; i++) {
                     if (correlator != null) {
                        correlator.registerRequest(this, currentMessage, messageAttributes);
                        sendMessage(sender, currentMessage, messageAttributes, mu);
                        waitForResponse.acquire(); // the only line throwing InterruptedException here
                        receivedMessage = new ReceivedMessage(correlatedResponse, messageToSend, currentMessage, messageAttributes);
                     } else {
                        receivedMessage = new ReceivedMessage(sendMessage(sender, currentMessage, messageAttributes, mu), messageToSend, currentMessage, messageAttributes);
                     }

                     if (receivedMessage.getResponse() != null) {
                        responseSize = responseSize + receivedMessage.getResponse().toString().length();
                     }

                     if (validationManager.isEnabled()) {
                        validationManager.submitValidationTask(new ValidationTask(Thread.currentThread().getName(), receivedMessage));
                     }
                  }
               }
            } else {
               if (correlator != null) {
                  final String error = "Receiver and Correlator cannot be used without a message definition. There is no information to compute and store the correlation ID. At least, define a message with an empty content.";
                  log.error(error);
                  reportSenderError(new PerfCakeException(error));
               } else {
                  receivedMessage = new ReceivedMessage(sendMessage(sender, null, messageAttributes, mu), null, null, messageAttributes);

                  if (receivedMessage.getResponse() != null) {
                     responseSize = responseSize + receivedMessage.getResponse().toString().length();
                  }

                  if (validationManager.isEnabled()) {
                     validationManager.submitValidationTask(new ValidationTask(Thread.currentThread().getName(), receivedMessage));
                  }
               }
            }

            senderManager.releaseSender(sender); // !!! important !!!
            sender = null;

            mu.appendResult(PerfCakeConst.REQUEST_SIZE_TAG, requestSize);
            mu.appendResult(PerfCakeConst.RESPONSE_SIZE_TAG, responseSize);

            reportManager.report(mu);
         }
      } catch (final Exception e) {
         if (e instanceof InterruptedException) { // there is just one line of code that can throw this exception above
            if (interruptWarningDisplayed.getAndIncrement() == 0) {
               log.warn("Test execution interrupted while waiting for a response message from a receiver.");
            }
         } else {
            log.error("Error sending message: ", e);
         }
      } finally {
         if (sender != null) {
            senderManager.releaseSender(sender);
         }
      }
   }

   /**
    * Notifies the sender task of receiving a response from a separate message channel.
    * This is called from {@link org.perfcake.message.correlator.Correlator} when {@link org.perfcake.message.receiver.Receiver} is used.
    *
    * @param response
    *       The response corresponding to the original request.
    */
   public void registerResponse(final Serializable response) {
      correlatedResponse = response;
      waitForResponse.release();
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
    * Configures a {@link SequenceManager} for the sender task.
    *
    * @param sequenceManager
    *       {@link SequenceManager} to be used by the sender task.
    */
   public void setSequenceManager(final SequenceManager sequenceManager) {
      this.sequenceManager = sequenceManager;
   }

   /**
    * Sets the correlator that is used to notify us about receiving a response from a separate message channel.
    *
    * @param correlator
    *       The correlator to be used.
    */
   public void setCorrelator(final Correlator correlator) {
      waitForResponse = new Semaphore(0);
      this.correlator = correlator;
   }
}
