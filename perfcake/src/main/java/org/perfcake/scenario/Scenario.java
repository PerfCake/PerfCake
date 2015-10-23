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
package org.perfcake.scenario;

import org.perfcake.PerfCakeException;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.MessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.message.sequence.SequenceManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.util.Utils;
import org.perfcake.validation.ValidationException;
import org.perfcake.validation.ValidationManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Encapsulates whole test execution, contains all information necessary to run the test.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Scenario {

   /**
    * Logger for this class.
    */
   private static final Logger log = LogManager.getLogger(Scenario.class);

   /**
    * Message generator.
    */
   private MessageGenerator generator;

   /**
    * Manager of message senders.
    */
   private MessageSenderManager messageSenderManager;

   /**
    * Report manager.
    */
   private ReportManager reportManager;

   /**
    * Store of the messages.
    */
   private List<MessageTemplate> messageStore;

   /**
    * Validation manager.
    */
   private ValidationManager validationManager;

   /**
    * Sequence manager.
    */
   private SequenceManager sequenceManager;

   /**
    * Initializes the scenario execution.
    *
    * @throws org.perfcake.PerfCakeException
    *       When it was not possible to fully initialize the scenario.
    */
   public void init() throws PerfCakeException {
      if (log.isTraceEnabled()) {
         log.trace("Scenario initialization...");
      }

      Utils.initTimeStamps();

      generator.setReportManager(reportManager);
      generator.setValidationManager(validationManager);
      generator.setSequenceManager(sequenceManager);

      try {
         generator.init(messageSenderManager, messageStore);
      } catch (final Exception e) {
         throw new PerfCakeException("Cannot initialize message generator: ", e);
      }
   }

   /**
    * Executes the scenario. This mainly means to send the messages.
    *
    * @throws PerfCakeException
    *       When it was not possible to execute the scenario.
    */
   public void run() throws PerfCakeException {
      if (log.isTraceEnabled()) {
         log.trace("Running scenario...");
      }

      if (validationManager.isEnabled()) {
         validationManager.startValidation();
      }

      try {
         generator.generate();
      } catch (final Exception e) {
         throw new PerfCakeException("Error generating messages: ", e);
      }
   }

   /**
    * Stops the scenario execution.
    */
   public void stop() {
      reportManager.stop();
   }

   /**
    * Finalizes the scenario.
    *
    * @throws PerfCakeException
    *       When it was not possible to perform all finalization operations.
    */
   public void close() throws PerfCakeException {
      if (generator != null) {
         generator.close();
      }

      try {
         validationManager.waitForValidation();
      } catch (final InterruptedException ie) {
         throw new PerfCakeException("Could not finish messages response validation properly: ", ie);
      }

      if (log.isTraceEnabled()) {
         if (validationManager.isAllMessagesValid()) {
            log.trace("Scenario finished successfully!");
         } else {
            log.trace("Scenario finished but there were validation errors.");
            throw new ValidationException("Some messages did not pass validation, please check validation log.");
         }
      }
   }

   /**
    * Checks if all threads used for message generating were terminated successfully.
    *
    * @return True if all threads were terminated.
    */
   public boolean areAllThreadsTerminated() {
      return generator.getActiveThreadsCount() <= 0;
   }

   /**
    * Gets the current {@link org.perfcake.model.Scenario.Generator}.
    *
    * @return The current {@link org.perfcake.model.Scenario.Generator}.
    */
   MessageGenerator getGenerator() {
      return generator;
   }

   /**
    * Sets the {@link org.perfcake.model.Scenario.Generator} for the scenario.
    *
    * @param generator
    *       The {@link org.perfcake.model.Scenario.Generator} to be set.
    */
   void setGenerator(final MessageGenerator generator) {
      this.generator = generator;
   }

   /**
    * Gets the current {@link org.perfcake.message.sender.MessageSenderManager}.
    *
    * @return The current {@link org.perfcake.message.sender.MessageSenderManager}.
    */
   MessageSenderManager getMessageSenderManager() {
      return messageSenderManager;
   }

   /**
    * Sets the {@link org.perfcake.message.sender.MessageSenderManager}.
    *
    * @param messageSenderManager
    *       The {@link org.perfcake.message.sender.MessageSenderManager} to be set.
    */
   void setMessageSenderManager(final MessageSenderManager messageSenderManager) {
      this.messageSenderManager = messageSenderManager;
   }

   /**
    * Gets the current {@link org.perfcake.reporting.ReportManager}.
    *
    * @return The current {@link org.perfcake.reporting.ReportManager}.
    */
   ReportManager getReportManager() {
      return reportManager;
   }

   /**
    * Sets the {@link org.perfcake.reporting.ReportManager}.
    *
    * @param reportManager
    *       The {@link org.perfcake.reporting.ReportManager} to be set.
    */
   void setReportManager(final ReportManager reportManager) {
      this.reportManager = reportManager;
   }

   /**
    * Gets the current message store.
    *
    * @return The current message store.
    */
   List<MessageTemplate> getMessageStore() {
      return messageStore;
   }

   /**
    * Sets the message store.
    *
    * @param messageStore
    *       The message store to be set.
    */
   void setMessageStore(final List<MessageTemplate> messageStore) {
      this.messageStore = messageStore;
   }

   /**
    * Sets the {@link org.perfcake.validation.ValidationManager}.
    *
    * @param validationManager
    *       The {@link org.perfcake.validation.ValidationManager} to be set.
    */
   void setValidationManager(final ValidationManager validationManager) {
      this.validationManager = validationManager;
   }

   /**
    * Gets the current {@link org.perfcake.validation.ValidationManager}.
    *
    * @return The current {@link org.perfcake.validation.ValidationManager}.
    */
   ValidationManager getValidationManager() {
      return validationManager;
   }

   /**
    * Sets the current {@link SequenceManager}.
    *
    * @param sequenceManager
    *       The {@link SequenceManager} to be set.
    */
   public void setSequenceManager(final SequenceManager sequenceManager) {
      this.sequenceManager = sequenceManager;
   }

   /**
    * Gets the current {@link SequenceManager}.
    *
    * @return The current {@link SequenceManager}.
    */
   SequenceManager getSequenceManager() {
      return sequenceManager;
   }
}
