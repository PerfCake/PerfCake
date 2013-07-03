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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.perfcake.message.MessageToSend;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.parser.ScenarioParser;
import org.perfcake.reporting.ReportManager;
import org.perfcake.util.Utils;
import org.perfcake.validation.ValidatorManager;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class Scenario {

   public static final Logger log = Logger.getLogger(Scenario.class);
   private AbstractMessageGenerator generator;
   private MessageSenderManager messageSenderManager;
   private ReportManager reportManager;
   private List<MessageToSend> messageStore;

   private URL scenarioUrl;

   /**
    * Create the scenario object. Parse scenario location with replacing system properties placeholders,
    * treating it first as a scenario name under the default location and the a complete URL.
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
         scenarioUrl = parseScenarioLocation(scenario);
      } catch (MalformedURLException e) {
         throw new PerfCakeException("Cannot parse scenario configuration location: ", e);
      }

      log.info("Scenario configuration: " + scenarioUrl.toString());
   }

   /**
    * Parse the scenario configuration
    * 
    * @throws PerfCakeException in case of any error during parsing
    */
   public void parse() throws PerfCakeException {
      if (log.isTraceEnabled()) {
         log.trace("Parsing scenario " + scenarioUrl.toString());
      }
      
      try (ScenarioParser parser = new ScenarioParser(scenarioUrl)) {
         generator = parser.parseGenerator();
         messageSenderManager = parser.parseSender(Integer.valueOf(generator.getProperty("threads")));
         reportManager = parser.parseReporting();
         messageStore = parser.parseMessages();
         parser.parseValidation();
      } catch (IOException e) {
         // we don't mind the open resource
      }
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
    * Execute the scenario.
    * This mainly means to send the messages.
    * 
    * @throws PerfCakeException
    */
   public void run() throws PerfCakeException {
      if (log.isTraceEnabled()) {
         log.trace("Running scenario...");
      }
      
      ValidatorManager.startValidation();
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
      generator.close();
      ValidatorManager.setFinished(true);

      if (log.isTraceEnabled()) {
         log.trace("Scenario finished successfully!");
      }
   }

   /**
    * Parse the location of a scenario configuration. Replaces all property placeholders, tries it as a scenario
    * name and then as a URL.
    * 
    * @param scenario
    *           configuration location
    * @return URL representing the scenario location
    * @throws MalformedURLException
    *            if it is not possible to represent the location as a URL
    * @throws PerfCakeException
    *            if it is not possible to replace placeholders
    */
   private URL parseScenarioLocation(String scenario) throws MalformedURLException, PerfCakeException {
      String result = null;
      try (Scanner scanner = new Scanner(scenario)) {
         result = Utils.filterProperties(scanner.useDelimiter("\\Z").next());
      } catch (IOException e) {
         throw new PerfCakeException("Cannot parse scenario configuration location: ", e);
      }

      // is there a protocol specified? suppose just scenario name
      if (result.indexOf("://") < 0) {
         result = "file://" + Utils.getProperty("perfcake.scenarios.dir", Utils.resourcesDir.getAbsolutePath() + "/scenarios") + "/" + result + ".xml";
      }

      return new URL(result);
   }
}
