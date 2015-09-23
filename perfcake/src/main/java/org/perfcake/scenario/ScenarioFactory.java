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

import java.net.URL;

/**
 * Interface of factories that can load a scenario from various resources.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface ScenarioFactory {

   /*
    * Default packages for classes implementing particular components.
    */
   String DEFAULT_GENERATOR_PACKAGE = "org.perfcake.message.generator";
   String DEFAULT_SEQUENCE_PACKAGE = "org.perfcake.message.sequence";
   String DEFAULT_SENDER_PACKAGE = "org.perfcake.message.sender";
   String DEFAULT_REPORTER_PACKAGE = "org.perfcake.reporting.reporters";
   String DEFAULT_DESTINATION_PACKAGE = "org.perfcake.reporting.destinations";
   String DEFAULT_VALIDATION_PACKAGE = "org.perfcake.validation";

   /**
    * Initializes all resources needed to prepare the scenario object. All I/O operations should happen here.
    *
    * @param scenarioURL
    *       Location of the scenario file.
    * @throws PerfCakeException
    *       When it was not possible to parse the scenario.
    */
   void init(final URL scenarioURL) throws PerfCakeException;

   /**
    * Constructs the scenario based on previously loaded data.
    *
    * @return Scenario instance specified in the file that has been loaded.
    * @throws PerfCakeException
    *       When it was not possible to properly parese the loaded data.
    */
   Scenario getScenario() throws PerfCakeException;
}
