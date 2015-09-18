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

import org.perfcake.TestSetup;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies basic integration of the validation framework into PerfCake.
 * It should confirm the contract and make sure the validation does not
 * disturb the measurement.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:BukovskyVaclav@centrum.cz">Václav Bukovský</a>
 */
public class ValidationIntegrationTest extends TestSetup {

   private ValidationManager getValidationManager(final Scenario scenario) {
      final ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      return retractor.getValidationManager();
   }

   @Test(enabled = true)
   public void basicIntegrationTest() throws Exception {
      final Scenario scenario = ScenarioLoader.load("test-validation-integration");

      final ValidationManager validationManager = getValidationManager(scenario);
      final DummyValidator v = (DummyValidator) validationManager.getValidator("v1");

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
      scenario.close();

      Assert.assertTrue(validationManager.isFastForward(), "Validation did not switch to fast forward.");

      lastCalled = v.getPreLastCalledTimestamp();
      lastCalled2 = v.getLastCalledTimestamp();
      timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff >= 1 && timeDiff < 100, String.format("Validation did not switch to normal speed operation (timeDiff = %d).", timeDiff));
   }

   /**
    * This test checks that the validators switch to fastForward once all messages are sent.
    */
   @Test(enabled = true)
   public void testEnableFastForward() throws Exception {
      final Scenario scenario = ScenarioLoader.load("test-validation-integration.xml");
      final ValidationManager validationManager = getValidationManager(scenario);
      final DummyValidator v = (DummyValidator) validationManager.getValidator("v1");

      scenario.init();
      scenario.run();

      Assert.assertFalse(validationManager.isFastForward(), "Validation switched to fast forward too soon.");

      scenario.close();

      Assert.assertTrue(validationManager.isFastForward(), "Validation did not switch to fast forward.");

      final long lastCalled = v.getPreLastCalledTimestamp();
      final long lastCalled2 = v.getLastCalledTimestamp();
      final long timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff >= 1 && timeDiff < 100, String.format("Validation did not switch to normal speed operation (timeDiff = %d).", timeDiff));
   }

   /**
    * When FastForward is enabled, validator should run without pauses.
    */
   @Test(enabled = true)
   public void testDefaultEnableFastForward() throws Exception {
      final Scenario scenario = ScenarioLoader.load("test-enable-fast-forward.xml");
      final ValidationManager validationManager = getValidationManager(scenario);
      final DummyValidator v = (DummyValidator) validationManager.getValidator("v1");

      scenario.init();
      scenario.run();

      Assert.assertTrue(validationManager.isFastForward(), "Validation did not loaded properly.");

      long lastCalled = v.getPreLastCalledTimestamp();
      long lastCalled2 = v.getLastCalledTimestamp();
      long timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff >= 0 && timeDiff < 100, String.format("Validation did not switch to normal speed operation (timeDiff = %d).", timeDiff));

      scenario.close();

      Assert.assertTrue(validationManager.isFastForward(), "Validation switched off fastForward unexpectedly.");

      lastCalled = v.getPreLastCalledTimestamp();
      lastCalled2 = v.getLastCalledTimestamp();
      timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff >= 0 && timeDiff < 100, String.format("Validation did not switch to normal speed operation (timeDiff = %d).", timeDiff));
   }

   /**
    * When the scenario is completed, all the validation completes successfully.
    */
   @Test(enabled = true)
   public void testFinishAllValidation() throws Exception {
      final Scenario scenario = ScenarioLoader.load("test-validation-multiple-validators.xml");
      final ValidationManager validationManager = getValidationManager(scenario);

      scenario.init();
      scenario.run();

      scenario.close();

      Assert.assertTrue(validationManager.isFinished());
      Assert.assertTrue(validationManager.messagesToBeValidated() == 0, "Validator could not validate all messages.");
      Assert.assertTrue(validationManager.isAllMessagesValid(), "One of the validation was not successful.");
      Assert.assertEquals(validationManager.getOverallStatistics().getPassed(), 37);
      Assert.assertEquals(validationManager.getOverallStatistics().getFailed(), 0);
   }

   /**
    * Negative test, some validations fail.
    */
   @Test(enabled = true)
   public void testFinishValidationWithError() throws Exception {
      final Scenario scenario = ScenarioLoader.load("test-using-wrong-validators.xml");
      final ValidationManager validationManager = getValidationManager(scenario);

      scenario.init();
      scenario.run();

      scenario.close();

      Assert.assertTrue(validationManager.isFinished(), "Validator should have been finished by now.");
      Assert.assertTrue(validationManager.messagesToBeValidated() == 0, "Validator could not validate all messages.");
      Assert.assertFalse(validationManager.isAllMessagesValid(), "All validation is correct but we sent messages that should have failed.");
      Assert.assertEquals(validationManager.getOverallStatistics().getPassed(), 13);
      Assert.assertEquals(validationManager.getOverallStatistics().getFailed(), 30);
   }
}