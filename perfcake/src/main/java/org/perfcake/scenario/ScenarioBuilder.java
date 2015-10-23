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
import org.perfcake.RunInfo;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.MessageGenerator;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.message.sequence.Sequence;
import org.perfcake.message.sequence.SequenceManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.util.ObjectFactory;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidationManager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * A Java based builder for creating {@link org.perfcake.scenario.Scenario} instance, which can be run by {@link org.perfcake.ScenarioExecution}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ScenarioBuilder {

   private Scenario scenario;

   /**
    * Gets a new ScenarioBuilder instance. Mandatory objects must be passed in.
    *
    * @param runInfo
    *       RunInfo specifying the test run time.
    * @param messageGenerator
    *       Message generator to be used to generate messages during test.
    * @param messageSender
    *       Sender template which will be copied to create all sender instances. Only the bean properties with proper get methods will be set on the new sender instances.
    * @throws PerfCakeException
    *       When any of the parameters are not set or creation of the underlying classes fails.
    */
   public ScenarioBuilder(final RunInfo runInfo, final MessageGenerator messageGenerator, final MessageSender messageSender) throws PerfCakeException {
      if (runInfo == null) {
         throw new PerfCakeException("RunInfo is not set.");
      }
      if (messageGenerator == null) {
         throw new PerfCakeException("Generator is not set.");
      }
      if (messageSender == null) {
         throw new PerfCakeException("Sender is not set.");
      }

      Properties props;
      try {
         props = ObjectFactory.getObjectProperties(messageSender);
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
         throw new PerfCakeException(String.format("Cannot access properties of the message sender template '%s':", messageSender.toString()), e);
      }

      initScenario(runInfo, messageGenerator, messageSender.getClass().getName(), props);
   }

   /**
    * Gets a new ScenarioBuilder instance. Mandatory objects must be passed in. Sender can be described as a class name and properties.
    *
    * @param runInfo
    *       RunInfo specifying the test run time.
    * @param messageGenerator
    *       Message generator to be used to generate messages during test.
    * @param senderClass
    *       Name of the sender class, instances will be used to send message in the scenario.
    * @param senderProperties
    *       Properties that will be set on the sender instances.
    * @throws PerfCakeException
    *       When any of the parameters are not set or creation of the underlying classes fails.
    */
   public ScenarioBuilder(final RunInfo runInfo, final MessageGenerator messageGenerator, final String senderClass, final Properties senderProperties) throws PerfCakeException {
      if (runInfo == null) {
         throw new PerfCakeException("RunInfo is not set.");
      }
      if (messageGenerator == null) {
         throw new PerfCakeException("Generator is not set.");
      }
      if (senderClass == null) {
         throw new PerfCakeException("Sender is not set.");
      }

      initScenario(runInfo, messageGenerator, senderClass, senderProperties);
   }

   private void initScenario(final RunInfo runInfo, final MessageGenerator messageGenerator, final String senderClass, final Properties senderProperties) throws PerfCakeException {
      scenario = new Scenario();
      messageGenerator.setRunInfo(runInfo);
      scenario.setGenerator(messageGenerator);

      final MessageSenderManager messageSenderManager = new MessageSenderManager();
      messageSenderManager.setSenderClass(senderClass);
      messageSenderManager.addMessageSenderProperties(senderProperties);
      messageSenderManager.setSenderPoolSize(messageGenerator.getThreads());
      scenario.setMessageSenderManager(messageSenderManager);

      final ReportManager reportManager = new ReportManager();
      reportManager.setRunInfo(runInfo);
      scenario.setReportManager(reportManager);

      scenario.setMessageStore(new ArrayList<MessageTemplate>());
      scenario.setValidationManager(new ValidationManager());

      scenario.setSequenceManager(new SequenceManager());
   }

   /**
    * Adds a {@link Reporter}, which will be used in {@link org.perfcake.scenario.Scenario} for reporting results. More reporters can be added
    *
    * @param r
    *       implementation
    * @return Instance of this for fluent API.
    */
   public ScenarioBuilder addReporter(final Reporter r) {
      scenario.getReportManager().registerReporter(r);
      return this;
   }

   /**
    * Adds a {@link MessageTemplate}, which will be used in the {@link org.perfcake.scenario.Scenario}
    *
    * @param messageTemplate
    *       A message template to be added to the list of messages to be send during in one sender cycle.
    * @return Instance of this for fluent API.
    */
   public ScenarioBuilder addMessage(final MessageTemplate messageTemplate) {
      scenario.getMessageStore().add(messageTemplate);
      return this;
   }

   /**
    * Puts a validator under the given key.
    *
    * @param validatorId
    *       Id of the new validator.
    * @param messageValidator
    *       The message validator to be registered.
    * @return Instance of this for fluent API.
    */
   public ScenarioBuilder putMessageValidator(final String validatorId, final MessageValidator messageValidator) {
      scenario.getValidationManager().addValidator(validatorId, messageValidator);
      scenario.getValidationManager().setEnabled(true);
      return this;
   }

   /**
    * Registers a new sequence under the given property name.
    *
    * @param sequenceName
    *       The name of the sequence (the name of the placeholder).
    * @param sequence
    *       The new sequence to be registered.
    * @return Instance of this for fluent API.
    * @throws PerfCakeException
    *       When it was not possible to properly initialize the newly added sequence.
    */
   public ScenarioBuilder putSequence(final String sequenceName, final Sequence sequence) throws PerfCakeException {
      scenario.getSequenceManager().addSequence(sequenceName, sequence);
      return this;
   }

   /**
    * Builds the usable {@link org.perfcake.scenario.Scenario} object, which can be then used for executing the scenario.
    *
    * @return The finished {@link org.perfcake.scenario.Scenario}.
    */
   public Scenario build() {
      return scenario;
   }

}
