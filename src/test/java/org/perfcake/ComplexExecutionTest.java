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

import org.perfcake.message.sender.DummySender;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test complete scenario execution.
 *
 * @author Martin Večera <marvenec@gmail.com>
 */
public class ComplexExecutionTest extends TestSetup {

   @Test
   public void iterationScenarioTest() throws Exception {
      final Scenario scenario;
      scenario = ScenarioLoader.load("test-iteration-scenario");
      scenario.init();
      scenario.run();
      scenario.close();

      Assert.assertEquals(DummySender.getCounter(), 1);
   }

}