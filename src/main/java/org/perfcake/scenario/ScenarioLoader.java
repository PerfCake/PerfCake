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

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.util.Utils;

import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Builder class for creating {@link org.perfcake.scenario.Scenario} instance, which can be run by {@link org.perfcake.ScenarioExecution}
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ScenarioLoader {

   public static final Logger log = Logger.getLogger(ScenarioBuilder.class);

   /**
    * Loads {@link org.perfcake.scenario.Scenario} from the location specified with the system property <code>-Dscenario=<scenario name></code>
    *
    * @param scenario
    *       scenario location
    * @return parsed scenario.
    * @throws PerfCakeException
    *       if scenario property is not set or there is some problem with loading the scenario.
    */
   public Scenario load(String scenario) throws PerfCakeException {
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

      String extension = null;
      int lastDot = scenarioUrl.toString().lastIndexOf(".");
      if (lastDot > -1) {
         extension = scenarioUrl.toString().substring(lastDot + 1).toLowerCase();
      }

      ScenarioFactory scenarioFactory = getFactory(extension);
      scenarioFactory.init(scenarioUrl);

      return scenarioFactory.getScenario();
   }

   private ScenarioFactory getFactory(String extension) throws PerfCakeException {
      switch (extension) {
         case "xml":
            return new XMLFactory();
         default:
            throw new PerfCakeException(String.format("Unknown scenario type %s", extension));
      }
   }

}
