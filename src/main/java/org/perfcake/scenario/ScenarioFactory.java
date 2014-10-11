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
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public interface ScenarioFactory {

   static final String DEFAULT_GENERATOR_PACKAGE = "org.perfcake.message.generator";
   static final String DEFAULT_SENDER_PACKAGE = "org.perfcake.message.sender";
   static final String DEFAULT_REPORTER_PACKAGE = "org.perfcake.reporting.reporters";
   static final String DEFAULT_DESTINATION_PACKAGE = "org.perfcake.reporting.destinations";
   static final String DEFAULT_VALIDATION_PACKAGE = "org.perfcake.validation";

   public void init(final URL scenarioURL) throws PerfCakeException;

   public Scenario getScenario() throws PerfCakeException;
}
