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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Tests the rules validator.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RulesValidatorTest {

   final Properties emptyProperties = new Properties();

   @Test
   public void rulesValidatorTest() throws IOException {
      final RulesValidator rv = new RulesValidator();

      final Message m = new Message();
      m.setPayload("Kdo šel z depa? Přece Pepa!");

      final Message mFail = new Message();
      mFail.setPayload("Kdo šel z depa? Přece Radek!");

      final File f = File.createTempFile("perfcake", "rules-validator");
      f.deleteOnExit();
      try (FileWriter fw = new FileWriter(f)) {
         fw.write("Message body contains \"Pepa\".");
         fw.flush();
         fw.close();

         rv.setRules(f.getAbsolutePath());
         Assert.assertTrue(rv.isValid(null, m, emptyProperties));
         Assert.assertTrue(rv.isValid(null, m, emptyProperties)); // the validation is repeatable
         Assert.assertTrue(rv.isValid(mFail, m, emptyProperties)); // we check for the response, not the original message

         Assert.assertFalse(rv.isValid(null, mFail, emptyProperties));
      } finally {
         f.delete();
      }
   }
}
