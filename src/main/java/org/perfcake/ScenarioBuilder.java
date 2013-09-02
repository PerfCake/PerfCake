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
package org.perfcake;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.sender.AbstractSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.model.ScenarioFactory;
import org.perfcake.parser.ScenarioParser;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.util.Utils;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidatorManager;

/**
 * Builder class for creating {@link Scenario} instance, which can be run by {@link ScenarioExecution}
 * 
 * Uses fluent API to setup builder.
 * 
 * @author Jiří Sedláček <jiri@sedlackovi.cz>
 */
public class ScenarioBuilder {

   public static final Logger log = Logger.getLogger(ScenarioBuilder.class);

   private RunInfo runInfo;
   private AbstractSender senderTemplate;
   private List<Reporter> reporters = new ArrayList<>();
   private List<MessageTemplate> messages = new ArrayList<>();
   private AbstractMessageGenerator generator;
   private ValidatorManager validatorManager = new ValidatorManager();

   private ReportManager reportManager;

   private MessageSenderManager messageSenderManager;

   public ScenarioBuilder() throws PerfCakeException {

   }

   /**
    * Sets message generator which will be used for {@link Scenario}
    * 
    * @param any
    *           message generator
    * @return
    */
   public ScenarioBuilder setGenerator(AbstractMessageGenerator g) {
      this.generator = g;
      return this;
   }

   /**
    * Sets {@link RunInfo} object, which will be used for {@link Scenario}
    * 
    * @param ri
    * @return
    */
   public ScenarioBuilder setRunInfo(RunInfo ri) {
      this.runInfo = ri;
      return this;
   }

   /**
    * Sets {@link AbstractSender} implementation object, which will be used as a template for preparing pool of senders
    * 
    * @param s
    * @return
    */
   public ScenarioBuilder setSender(AbstractSender s) {
      this.senderTemplate = s;
      return this;
   }

   /**
    * Adds a {@link Reporter}, which will be used in {@link Scenario} for reporting results. More reporters can be added
    * 
    * @param r
    * @return
    */
   public ScenarioBuilder addReporter(Reporter r) {
      reporters.add(r);
      return this;
   }

   /**
    * Adds a {@link MessageTemplate}, which will be used in {@link Scenario}
    * 
    * @param message
    * @return
    */
   public ScenarioBuilder addMessage(MessageTemplate message) {
      messages.add(message);
      return this;
   }

   /**
    * Put validator under the key validatorId
    * 
    * @param validatorId
    * @param message
    *           validator
    * @return
    */
   public ScenarioBuilder putMessageValidator(String validatorId, MessageValidator mv) {
      validatorManager.addValidator(validatorId, mv);
      validatorManager.setEnabled(true);
      return this;
   }

   /**
    * Builds the usable {@link Scenario} object, which can be then used for executing the scenario.
    * 
    * @return
    * @throws Exception
    *            if {@link RunInfo} is not set
    * @throws Exception
    *            if some generator is not set
    * @throws Exception
    *            if some sender is not set
    */
   public Scenario build() throws Exception {
      if (runInfo == null)
         throw new IllegalStateException("RunInfo is not set");
      if (generator == null)
         throw new IllegalStateException("Generator is not set");
      if (messageSenderManager == null && senderTemplate == null)
         throw new IllegalStateException("Sender is not set");

      Scenario sc = new Scenario();
      generator.setRunInfo(runInfo);
      sc.setGenerator(generator);

      if (messageSenderManager == null) {
         MessageSenderManager msm = new MessageSenderManager();
         msm.setSenderPoolSize(generator.getThreads());
         for (int i = 0; i < generator.getThreads(); i++) {
            AbstractSender newInstance = senderTemplate.getClass().newInstance();
            BeanUtils.copyProperties(newInstance, senderTemplate);
            newInstance.init();
            msm.addSenderInstance(newInstance);
         }
         sc.setMessageSenderManager(msm);
      } else {
         sc.setMessageSenderManager(messageSenderManager);
      }

      if (reportManager == null) {// if report manager created programaticaly
         ReportManager rm = new ReportManager();
         rm.setRunInfo(runInfo);
         for (Reporter r : reporters) {
            rm.registerReporter(r);
         }
         sc.setReportManager(rm);
      } else { // if report manager parsed directly
         reportManager.setRunInfo(runInfo);
         sc.setReportManager(reportManager);
      }

      sc.setMessageStore(messages);
      sc.setValidatorManager(validatorManager);

      return sc;
   }

   /**
    * Loads all data needed for {@link Scenario} from JAXB model, build method may be called afterwards.
    * 
    * @param model
    * @return
    * @throws PerfCakeException
    */
   public ScenarioBuilder load(org.perfcake.model.Scenario model) throws PerfCakeException {
      ScenarioFactory sf = new ScenarioFactory(model);

      runInfo = sf.parseRunInfo();
      generator = sf.parseGenerator();
      messageSenderManager = sf.parseSender(generator.getThreads());
      reportManager = sf.parseReporting();
      validatorManager = sf.parseValidation();
      messages = sf.parseMessages(validatorManager);

      return this;
   }

   /**
    * loads {@link Scenario} from system property <code>-Dscenario=<scenario name></code>
    * 
    * @param scenario
    * @return
    * @throws PerfCakeException
    *            if scenario property is not set or there is some problem with parsing the xml document describing the scenario
    */
   public ScenarioBuilder load(String scenario) throws PerfCakeException {
      if (scenario == null) {
         throw new PerfCakeException("Scenario property is not set. Please use -Dscenario=<scenario name> to specify a scenario.");
      }

      URL scenarioUrl = null;
      try {
         scenarioUrl = Utils.locationToUrl(scenario, PerfCakeConst.SCENARIOS_DIR_PROPERTY, Utils.determineDefaultLocation("scenarios"), ".xml");
      } catch (MalformedURLException e) {
         throw new PerfCakeException("Cannot parse scenario configuration location: ", e);
      }

      log.info("Scenario configuration: " + scenarioUrl.toString());

      if (log.isTraceEnabled()) {
         log.trace("Parsing scenario " + scenarioUrl.toString());
      }

      org.perfcake.model.Scenario model = new ScenarioParser(scenarioUrl).parse();

      return load(model);
   }

}
