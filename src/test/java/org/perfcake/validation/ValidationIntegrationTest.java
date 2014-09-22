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
package org.perfcake.validation;

import org.perfcake.PerfCakeConst;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

/**
 * Verifies basic integration of the validation framework into PerfCake.
 * It should confirm the contract and make sure the validation does not
 * disturb the measurement.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ValidationIntegrationTest {

   @Test(enabled = false)
   public void basicIntegrationTest() throws Exception {
      System.setProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY, getClass().getResource("/scenarios").getPath());
      System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, getClass().getResource("/messages").getPath());
      Scenario scenario = new ScenarioLoader().load("test-validation-integration");

      // to avoid complicated scenario construction, we relay on public API and dig into it then
      Field vmField = scenario.getClass().getDeclaredField("validationManager");
      vmField.setAccessible(true);
      ValidationManager validationManager = (ValidationManager) vmField.get(scenario);
      DummyValidator v = (DummyValidator) validationManager.getValidator("v1");

      // first, the validation must not run very fast while the measurement is in progress
      scenario.init();
      scenario.run();

      long timeout = System.currentTimeMillis() + 1000;
      while (v.getLastCalledTimestamp() == 0 && timeout > System.currentTimeMillis()) {
         Thread.sleep(10);
      }
      long lastCalled = v.getLastCalledTimestamp();

      timeout = System.currentTimeMillis() + 1000;
      while (v.getLastCalledTimestamp() == lastCalled && timeout > System.currentTimeMillis()) {
         Thread.sleep(10);
      }
      long lastCalled2 = v.getLastCalledTimestamp();
      long timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff >= 450, "Validator called to often during running measurement.");

      // after we stop the measurement, the validation must switch to full speed
      scenario.stop();
      lastCalled = v.getLastCalledTimestamp();
      Thread.sleep(10);
      lastCalled2 = v.getLastCalledTimestamp();
      timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff > 1 && timeDiff < 20, "Validator did not switch to normal speed operation.");

      scenario.close();
   }
}
