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
package org.perfcake.scenario;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Loads {@link org.perfcake.scenario.Scenario} instance, which can be run by {@link org.perfcake.ScenarioExecution} based on its file type.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ScenarioLoader {

   private static final Logger log = LogManager.getLogger(ScenarioBuilder.class);

   /**
    * Loads {@link org.perfcake.scenario.Scenario} from the location specified with the system property <code>-Dscenario=&lt;scenario name&gt;</code>.
    *
    * @param scenario
    *       Scenario location.
    * @return Parsed {@link org.perfcake.scenario.Scenario}.
    * @throws PerfCakeException
    *       If scenario property is not set or there is some problem with loading the scenario.
    */
   public static Scenario load(final String scenario) throws PerfCakeException {
      if (scenario == null) {
         throw new PerfCakeException("Scenario property is not set. Please use -Dscenario=<scenario name> to specify a scenario.");
      }

      final URL scenarioUrl;
      try {
         scenarioUrl = Utils.locationToUrlWithCheck(scenario, PerfCakeConst.SCENARIOS_DIR_PROPERTY, Utils.determineDefaultLocation("scenarios"), ".xml", ".dsl");
      } catch (final MalformedURLException e) {
         throw new PerfCakeException("Cannot parse scenario configuration location: ", e);
      }

      log.info("Scenario configuration: " + scenarioUrl.toString());

      if (log.isTraceEnabled()) {
         log.trace("Parsing scenario " + scenarioUrl.toString());
      }

      String extension = "UNKNOWN";
      final int lastDot = scenarioUrl.toString().lastIndexOf(".");
      if (lastDot > -1) {
         extension = scenarioUrl.toString().substring(lastDot + 1).toLowerCase();
      }

      final ScenarioFactory scenarioFactory = getFactory(extension);
      scenarioFactory.init(scenarioUrl);

      final Scenario result = scenarioFactory.getScenario();
      result.getReportManager().getRunInfo().setScenarioName(scenarioUrl.toString());

      return result;
   }

   private static ScenarioFactory getFactory(final String extension) throws PerfCakeException {
      switch (extension) {
         case "xml":
            return new XmlFactory();
         case "dsl":
            return new DslFactory();
         default:
            throw new PerfCakeException(String.format("Unknown scenario type %s", extension));
      }
   }

}
