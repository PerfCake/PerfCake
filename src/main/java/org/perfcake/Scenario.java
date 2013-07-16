/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.parser.ScenarioParser;
import org.perfcake.reporting.ReportManager;
import org.perfcake.util.Utils;
import org.perfcake.validation.ValidatorManager;

/**
 * 
 * TODO Add public API for use without a scenario in XML
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class Scenario {

   public static final Logger log = Logger.getLogger(Scenario.class);
   private AbstractMessageGenerator generator;
   private MessageSenderManager messageSenderManager;
   private ReportManager reportManager;
   private List<MessageTemplate> messageStore;

   public static final String VERSION = "0.2";

   private URL scenarioUrl;

   /**
    * Create the scenario object. Parse scenario location with replacing system
    * properties placeholders, treating it first as a scenario name under the
    * default location and the a complete URL.
    * 
    * @param scenario
    *           scenario name or location URL
    * @throws PerfCakeException
    */
   public Scenario(String scenario) throws PerfCakeException {
      if (scenario == null) {
         throw new PerfCakeException("Scenario property is not set. Please use -Dscenario=<scenario name> to specify a scenario.");
      }

      try {
         scenarioUrl = Utils.locationToUrl(scenario, "perfcake.scenarios.dir", Utils.determineDefaultLocation("scenarios"), ".xml");
      } catch (MalformedURLException e) {
         throw new PerfCakeException("Cannot parse scenario configuration location: ", e);
      }

      log.info("Scenario configuration: " + scenarioUrl.toString());
   }

   /**
    * Parse the scenario configuration
    * 
    * @throws PerfCakeException
    *            in case of any error during parsing
    */
   public void parse() throws PerfCakeException {
      if (log.isTraceEnabled()) {
         log.trace("Parsing scenario " + scenarioUrl.toString());
      }

      ScenarioParser parser = new ScenarioParser(scenarioUrl);
      generator = parser.parseGenerator();
      messageSenderManager = parser.parseSender(generator.getThreads());
      reportManager = parser.parseReporting();
      messageStore = parser.parseMessages();
      parser.parseValidation();
   }

   /**
    * Initialize the scenario execution
    * 
    * @throws PerfCakeException
    */
   public void init() throws PerfCakeException {
      if (log.isTraceEnabled()) {
         log.trace("Scenario initialization...");
      }

      messageSenderManager.setReportManager(reportManager);
      generator.setReportManager(reportManager);
      try {
         generator.init(messageSenderManager, messageStore);
      } catch (Exception e) {
         throw new PerfCakeException("Cannot initialize message generator: ", e);
      }
   }

   /**
    * Execute the scenario. This mainly means to send the messages.
    * 
    * @throws PerfCakeException
    */
   public void run() throws PerfCakeException {
      if (log.isTraceEnabled()) {
         log.trace("Running scenario...");
      }

      if (ValidatorManager.isEnabled()) {
         ValidatorManager.startValidation();
      }
      try {
         generator.generate();
      } catch (Exception e) {
         throw new PerfCakeException("Error generating messages: ", e);
      }
   }

   /**
    * Finalize the scenario.
    * 
    * @throws PerfCakeException
    */
   public void close() throws PerfCakeException {
      if (generator != null)
         generator.close();
      ValidatorManager.setFinished(true);

      if (log.isTraceEnabled()) {
         log.trace("Scenario finished successfully!");
      }
   }
}
