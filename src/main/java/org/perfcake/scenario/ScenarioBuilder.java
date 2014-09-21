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
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidationManager;

import java.util.ArrayList;

/**
 * A Java based builder for creating {@link org.perfcake.scenario.Scenario} instance, which can be run by {@link org.perfcake.ScenarioExecution}.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ScenarioBuilder {

   private Scenario scenario;

   /**
    * Gets a new ScenarioBuilder instance. Mandatory objects must be passed in.
    *
    * @param runInfo RunInfo specifying the test run time.
    * @param messageGenerator Message generator to be used to generate messages during test.
    * @param senderTemplate Sender template which will be cloned to create all sender instances.
    * @throws PerfCakeException When any of the parameters are not set or creation of the underlying classes fails.
    */
   public ScenarioBuilder(final RunInfo runInfo, final AbstractMessageGenerator messageGenerator, final MessageSender senderTemplate) throws PerfCakeException {
      if (runInfo == null) {
         throw new PerfCakeException("RunInfo is not set.");
      }
      if (messageGenerator == null) {
         throw new PerfCakeException("Generator is not set.");
      }
      if (senderTemplate == null) {
         throw new PerfCakeException("Sender is not set.");
      }

      scenario = new Scenario();
      messageGenerator.setRunInfo(runInfo);
      scenario.setGenerator(messageGenerator);

      MessageSenderManager messageSenderManager = new MessageSenderManager();
      messageSenderManager.setSenderClass(senderTemplate.getClass().getName());
      messageSenderManager.setSenderPoolSize(messageGenerator.getThreads());
      scenario.setMessageSenderManager(messageSenderManager);

      ReportManager reportManager = new ReportManager();
      reportManager.setRunInfo(runInfo);
      scenario.setReportManager(reportManager);

      scenario.setMessageStore(new ArrayList<MessageTemplate>());
      scenario.setValidationManager(new ValidationManager());
   }

   /**
    * Adds a {@link Reporter}, which will be used in {@link org.perfcake.scenario.Scenario} for reporting results. More reporters can be added
    *
    * @param Reporter
    *       implementation
    * @return this
    */
   public ScenarioBuilder addReporter(final Reporter r) {
      scenario.getReportManager().registerReporter(r);
      return this;
   }

   /**
    * Adds a {@link MessageTemplate}, which will be used in the {@link org.perfcake.scenario.Scenario}
    *
    * @param MessageTemplate A message template to be added to the list of messages to be send during in one sender cycle.
    * @return this
    */
   public ScenarioBuilder addMessage(final MessageTemplate messageTemplate) {
      scenario.getMessageStore().add(messageTemplate);
      return this;
   }

   /**
    * Puts a validator under the given key.
    *
    * @param validatorId Id of the new validator.
    * @param messageValidator The message validator to be registered.
    * @return this
    */
   public ScenarioBuilder putMessageValidator(final String validatorId, final MessageValidator messageValidator) {
      scenario.getValidationManager().addValidator(validatorId, messageValidator);
      scenario.getValidationManager().setEnabled(true);
      return this;
   }

   /**
    * Builds the usable {@link org.perfcake.scenario.Scenario} object, which can be then used for executing the scenario.
    *
    * @return The finished {@link org.perfcake.scenario.Scenario}.
    */
   public Scenario build() throws Exception {
      return scenario;
   }

}
