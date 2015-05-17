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

import org.perfcake.message.Message;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

/**
 * Tests the script validator.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ScriptValidatorTest {

   final Properties emptyProperties = new Properties();

   @Test
   public void testScriptValidator() {
      final Message m = new Message();
      m.setPayload("Kdo šel z depa? Přece Pepa!");

      final Message mFail = new Message();
      mFail.setPayload("Kdo šel z depa? Přece Radek!");

      final ScriptValidator sv = new ScriptValidator();
      sv.setEngine("groovy");
      sv.setScript("log.info('Be groovy!.....')\nreturn message.payload.toString().contains('Pepa')");

      Assert.assertTrue(sv.isValid(null, m, emptyProperties));
      Assert.assertTrue(sv.isValid(null, m, emptyProperties)); // make sure the validator is reusable
      Assert.assertFalse(sv.isValid(null, mFail, emptyProperties));
   }

}
