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
import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

/**
 * Verifies basic integration of the validation framework into PerfCake.
 * It should confirm the contract and make sure the validation does not
 * disturb the measurement.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ValidationIntegrationTest extends TestSetup {

   private Scenario scenario;
   private Field vmField;
   private ValidationManager validationManager;

   /*
    * Prepare paths before running the tests.
    */
   @BeforeTest
   public void prepareScenario() throws PerfCakeException {
      System.setProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY, getClass().getResource("/scenarios").getPath());
      System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, getClass().getResource("/messages").getPath());
   }

   /*
    * When performance test is running validation runs really only once per 0.5 seconds.
    */
   @Test(enabled = true)
   public void testValidationTimeout() throws Exception {
      scenario = ScenarioLoader.load("test-validation-integration");

      vmField = scenario.getClass().getDeclaredField("validationManager");
      vmField.setAccessible(true);
      validationManager = (ValidationManager) vmField.get(scenario);

      DummyValidator dv = (DummyValidator) validationManager.getValidator("v1");

      scenario.init();
      scenario.run();

      long timeout = System.currentTimeMillis() + 1000;
      while (dv.getLastCalledTimestamp() == 0 && timeout > System.currentTimeMillis()) {
         Thread.sleep(10);
      }
      long lastCalled = dv.getLastCalledTimestamp();

      timeout = System.currentTimeMillis() + 1000;
      while (dv.getLastCalledTimestamp() == lastCalled && timeout > System.currentTimeMillis()) {
         Thread.sleep(10);
      }
      long lastCalled2 = dv.getLastCalledTimestamp();
      long timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff >= 450, "Validator called to often during running measurement.");

      scenario.close();
   }

   /*
    * This test checks that the validators don't validated with enabled fastForward by default,
    * but only after completing the scenario.
    */
   @Test(enabled = true)
   public void testEnableFastForward() throws Exception {
      scenario = ScenarioLoader.load("test-validation-integration.xml");

      vmField = scenario.getClass().getDeclaredField("validationManager");
      vmField.setAccessible(true);
      validationManager = (ValidationManager) vmField.get(scenario);

      DummyValidator v = (DummyValidator) validationManager.getValidator("v1");

      scenario.init();
      scenario.run();

      Assert.assertTrue(!validationManager.isFastForward(), "Validation did not switch to fast forward.");

      scenario.close();

      Assert.assertTrue(validationManager.isFastForward(), "Validation did not switch to fast forward.");

      long lastCalled = v.getPreLastCalledTimestamp();
      long lastCalled2 = v.getLastCalledTimestamp();
      long timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff > 1 && timeDiff < 20, "Validation did not switch to normal speed operation.");
   }

   /*
    * When FastForward is enabled, validator don't validated once per 0.5 seconds, but validates without a break.
    */
   @Test(enabled = true)
   public void testDefaultEnableFastForward() throws Exception {
      scenario = ScenarioLoader.load("test-enable-fast-forward.xml");

      vmField = scenario.getClass().getDeclaredField("validationManager");
      vmField.setAccessible(true);
      validationManager = (ValidationManager) vmField.get(scenario);

      DummyValidator v = (DummyValidator) validationManager.getValidator("v1");

      long lastCalled = 0;
      long lastCalled2 = 0;
      long timeDiff = 0;

      scenario.init();
      scenario.run();

      Assert.assertTrue(validationManager.isFastForward(), "Validation did not switch to fast forward.");

      lastCalled = v.getPreLastCalledTimestamp();
      lastCalled2 = v.getLastCalledTimestamp();
      timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff > 1 && timeDiff < 20, "Validation did not switch to fast forward.");

      scenario.close();

      Assert.assertFalse(!validationManager.isFastForward(), "Validation did not switch to normal speed operation.");

      lastCalled = v.getPreLastCalledTimestamp();
      lastCalled2 = v.getLastCalledTimestamp();
      timeDiff = lastCalled2 - lastCalled;

      Assert.assertTrue(timeDiff > 1 && timeDiff < 20, "Validation did not switch to normal speed operation.");
   }

   /*
    * They only use the available validators and validators validate only scenarios that are assigned to them.
    */
   @Test(enabled = true)
   public void testCorrectValidatorsUse() throws Exception {
      scenario = ScenarioLoader.load("test-validation-integration.xml");

      vmField = scenario.getClass().getDeclaredField("validationManager");
      vmField.setAccessible(true);
      validationManager = (ValidationManager) vmField.get(scenario);

      Assert.assertTrue((validationManager.getValidator("v1")) != null, "Validator v1 is declared in test-validation-integration scenarion.");
      Assert.assertTrue((validationManager.getValidator("text1")) == null, "Validator text1 is not declared in test-validation-integration scenarion.");

      scenario = ScenarioLoader.load("test-validator-load.xml");

      vmField = scenario.getClass().getDeclaredField("validationManager");
      vmField.setAccessible(true);
      validationManager = (ValidationManager) vmField.get(scenario);

      Assert.assertTrue((validationManager.getValidator("v1")) == null, "Validator v1 is declared in test-validator-load scenarion.");
      Assert.assertTrue((validationManager.getValidator("text2")) != null, "Validator text1 is not declared in test-validator-load scenarion.");
   }

   /*
    * When the scenario is completed, all the validation completes successfully.
    * When the above scenario run multiple validations, all validations must performed and successfully completed.
    */
   @Test(enabled = true)
   public void testFinishAllValidation() throws Exception {
      scenario = ScenarioLoader.load("test-validation-multiple-validators.xml");

      vmField = scenario.getClass().getDeclaredField("validationManager");
      vmField.setAccessible(true);
      validationManager = (ValidationManager) vmField.get(scenario);

      scenario.init();
      scenario.run();

      Thread.sleep(10000);

      scenario.close();

      while (!validationManager.isFinished()){
         Thread.sleep(10);
      }

      Assert.assertTrue(validationManager.messagesToBeValidated() == 0, "Validator could not validate all messages.");
      Assert.assertTrue(validationManager.isAllMessagesValid(), "One of the validation was not successful.");
      Assert.assertTrue(validationManager.isAllValidatorsWithoutError(), "One of the validator ended with error.");
   }

   /*
    * When the scenario will validate by multiple validators at the same time and one mid fails,
    * the other validators successfully completes validation.
    */
   @Test(enabled = true)
   public void testFinishValidationWithError() throws Exception {
      int waitTime = 0;

      scenario = ScenarioLoader.load("test-using-wrong-validators.xml");

      vmField = scenario.getClass().getDeclaredField("validationManager");
      vmField.setAccessible(true);
      validationManager = (ValidationManager) vmField.get(scenario);

      scenario.init();
      scenario.run();

      Thread.sleep(10000);

      try {
         scenario.close();
         while (validationManager.messagesToBeValidated() != 0){
            Thread.sleep(10);
         }

         Assert.assertTrue(validationManager.isFinished(), "All messages is validated but validator could not finished.");
         Assert.assertFalse(validationManager.isAllMessagesValid(), "All validation is not correct.");
      } catch (PerfCakeException ex){
         while ((validationManager.messagesToBeValidated() != 0) && (waitTime < 10)){
            Thread.sleep(10);
            waitTime++;
         }

         Assert.assertTrue(validationManager.messagesToBeValidated() == 0, "Validator could not validate all messages.");
         Assert.assertTrue(validationManager.isFinished(), "The validation is not finished.");
         Assert.assertFalse(validationManager.isAllMessagesValid(), "All validation is not correct.");
         Assert.assertFalse(validationManager.isAllValidatorsWithoutError(), "One of the validator ended with error but it was not detected.");
      }
   }
}