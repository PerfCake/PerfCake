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
import org.perfcake.util.ObjectFactory;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

public class RegExpValidatorTest {

   final Properties emptyProperties = new Properties();

   @Test
   public void validationTest() {
      final RegExpValidator rev = new RegExpValidator();
      final Message m = new Message();

      m.setPayload("né pětku");
      rev.setPattern(".*pět[^k].");
      Assert.assertFalse(rev.isValid(null, m, emptyProperties));

      m.setPayload("zpětné");
      Assert.assertTrue(rev.isValid(null, m, emptyProperties));
   }

   @Test
   public void patternFlagCaseInsensitiveTest() {
      final RegExpValidator testedValidator = new RegExpValidator();
      final Message m = new Message();
      m.setPayload("Velké věci mají malé začátky.");
      testedValidator.setPattern("velké.*");

      Assert.assertFalse(testedValidator.isValid(null, m, emptyProperties));
      testedValidator.setCaseInsensitive(true);
      Assert.assertTrue(testedValidator.isValid(null, m, emptyProperties));
   }

   @Test
   public void patternFlagMultilineTest() {
      final RegExpValidator testedValidator = new RegExpValidator();
      final Message m = new Message();
      m.setPayload("první řádek\ndruhý řádek");
      testedValidator.setPattern("^první .*$.*^druhý.*$");

      Assert.assertFalse(testedValidator.isValid(null, m, emptyProperties));
      testedValidator.setMultiline(true);
      testedValidator.setDotall(true);
      Assert.assertTrue(testedValidator.isValid(null, m, emptyProperties));
   }

   @Test
   public void patternFlagDotAllTest() {
      final RegExpValidator testedValidator = new RegExpValidator();
      final Message m = new Message();
      m.setPayload("první řádek\ndruhý řádek\ntřetí řádek");
      testedValidator.setPattern(".*druhý řádek.*");

      Assert.assertFalse(testedValidator.isValid(null, m, emptyProperties));
      testedValidator.setDotall(true);
      Assert.assertTrue(testedValidator.isValid(null, m, emptyProperties));
   }

   @Test
   public void patternFlagUnicodeCaseTest() {
      final RegExpValidator testedValidator = new RegExpValidator();
      final Message m = new Message();
      m.setPayload("Velke VĚCI mají malé začátky.");
      testedValidator.setPattern(".*věci.*");

      Assert.assertFalse(testedValidator.isValid(null, m, emptyProperties));
      testedValidator.setCaseInsensitive(true);
      Assert.assertFalse(testedValidator.isValid(null, m, emptyProperties));
      testedValidator.setUnicodeCase(true);
      Assert.assertTrue(testedValidator.isValid(null, m, emptyProperties));
   }

   @Test
   public void patternFlagCanonEq() {
      final RegExpValidator testedValidator = new RegExpValidator();
      final Message m = new Message();
      m.setPayload("Vidím město veliké, jehož sláva hvězd se dotýká.");
      testedValidator.setPattern(".*me\u030csto.*");

      Assert.assertFalse(testedValidator.isValid(null, m, emptyProperties));
      testedValidator.setCanonEq(true);
      Assert.assertTrue(testedValidator.isValid(null, m, emptyProperties));
   }

   @Test
   public void patternFlagTestLiteral() {
      final RegExpValidator testedValidator = new RegExpValidator();
      final Message m = new Message();
      m.setPayload("[a-bA-Z].*");
      testedValidator.setPattern("[a-bA-Z].*");

      Assert.assertFalse(testedValidator.isValid(null, m, emptyProperties));
      testedValidator.setLiteral(true);
      Assert.assertTrue(testedValidator.isValid(null, m, emptyProperties));
   }

   @Test
   public void patternFlagTestUnicodeCharacterClass() {
      final RegExpValidator testedValidator = new RegExpValidator();
      final Message m = new Message();
      m.setPayload("Bedřich 3.");
      testedValidator.setPattern("\\w+ \\d+\\.");

      Assert.assertFalse(testedValidator.isValid(null, m, emptyProperties));
      testedValidator.setUnicodeCharacterClass(true);
      Assert.assertTrue(testedValidator.isValid(null, m, emptyProperties));
   }

   @Test
   public void patternFlagCommentsTest() {
      final RegExpValidator testedValidator = new RegExpValidator();
      final Message m = new Message();
      m.setPayload("Vidím město veliké, jehož sláva hvězd se dotýká.");
      testedValidator.setPattern(".*město.*,.*# comment");

      Assert.assertFalse(testedValidator.isValid(null, m, emptyProperties));
      testedValidator.setComments(true);
      Assert.assertTrue(testedValidator.isValid(null, m, emptyProperties));
   }

   @Test
   public void dynamicCreationTest() throws Exception {
      final String pattern = "Ich weiss nich was soll es bedeuten dass ich so traurig bin";
      final Properties p = new Properties();
      p.setProperty("pattern", pattern);

      final RegExpValidator r = (RegExpValidator) ObjectFactory.summonInstance(RegExpValidator.class.getName(), p);

      Assert.assertEquals(r.getPattern(), pattern);
   }
}
