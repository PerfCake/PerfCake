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

import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.MessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.message.sequence.SequenceManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.ValidationManager;

import java.util.List;

/**
 * Utility class for tests. It can retract scenario classes from Scenario object.
 * It is possible because this class is in the same package as Scenario. However,
 * this is just for test purposes. Also, it should be used only for tests, where there
 * is no other option of obtaining the references.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ScenarioRetractor {

   private final Scenario scenario;

   /**
    * Creates a new retractor to keep the Scenario insides visible to the outer world.
    *
    * @param scenario
    *       The scenario subject to inspection.
    */
   public ScenarioRetractor(final Scenario scenario) {
      this.scenario = scenario;
   }

   /**
    * Gets the scenario generator.
    *
    * @return The generator.
    */
   public MessageGenerator getGenerator() {
      return scenario.getGenerator();
   }

   /**
    * Gets the message sender manager.
    *
    * @return The message sender manager.
    */
   public MessageSenderManager getMessageSenderManager() {
      return scenario.getMessageSenderManager();
   }

   /**
    * Gets the report manager.
    *
    * @return The report manager.
    */
   public ReportManager getReportManager() {
      return scenario.getReportManager();
   }

   /**
    * Gets the validation manager.
    *
    * @return The validation manager.
    */
   public ValidationManager getValidationManager() {
      return scenario.getValidationManager();
   }

   /**
    * Gets the {@link SequenceManager}.
    *
    * @return The {@link SequenceManager}.
    */
   public SequenceManager getSequenceManager() {
      return scenario.getSequenceManager();
   }

   /**
    * Gets the message store.
    *
    * @return The list of all message templates.
    */
   public List<MessageTemplate> getMessageStore() {
      return scenario.getMessageStore();
   }
}
