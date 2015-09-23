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
import org.perfcake.message.Message;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class DictionaryValidatorTest {
   final static String SVRATKA = "Na břehu řeky Svratky kvete rozrazil,\n"
         + "na břehu řeky Svratky roste nízká tráva,\n"
         + "rád chodil jsem tam denně, koupal se a snil,\n"
         + "na břehu řeky Svratky kvete rozrazil\n"
         + "a voda je tu těžká, chladná, kalná, tmavá.\n"
         + "\n"
         + "I za slunného léta je zde zvláštní stín\n"
         + "jak v starém obraze, jenž u nás doma visí,\n"
         + "proč cítil jsem tu vonět kopr, česnek, kmín,\n"
         + "i za slunného léta je zde zvláštní stín\n"
         + "jak v jedné zahradě, kam chodíval jsem kdysi.\n"
         + "\n"
         + "Jsou možná hezčí řeky, mají větší třpyt\n"
         + "než tento teskný břeh, než temná řeka Svratka,\n"
         + "a přece musil jsem tu každoročně žít,\n"
         + "jsou možná hezčí řeky, mají větší třpyt,\n"
         + "však nechodila k jejich břehům moje matka.\n"
         + "\n"
         + "Jsou možná země, kde je voda modravá\n"
         + "a nebe modravé a hory modravější,\n"
         + "a přec mou zemí navždy bude Morava,\n"
         + "jsou možná země, kde je voda modravá,\n"
         + "a přec mi nejsou drahé jak ta země zdejší.\n"
         + "\n"
         + "Jsou možná mnohem nádhernější hřbitovy,\n"
         + "je Vyšehrad, ten zlatý klenot v srdci Prahy —\n"
         + "a přec mě nejvíc dojímá ten žulový,\n"
         + "jsou možná mnohem nádhernější hřbitovy,\n"
         + "a přec ten nad Brnem je nade vše mi drahý.\n"
         + "\n"
         + "Na břehu řeky Svratky kvete rozrazil\n"
         + "a v létě tyčí se tu kukuřičná zrna.\n"
         + "Ó kéž bych, matko, s tebou dodneška tu žil,\n"
         + "na břehu řeky Svratky kvete rozrazil,\n"
         + "kéž žil bych s tebou, matko, dodnes ve zdech Brna.\n"
         + "\n"
         + "Jsou možná hezčí řeky, mají větší třpyt\n"
         + "než tento teskný břeh, než temná řeka Svratka,\n"
         + "a přec bych chtěl tu, matko, s tebou věčně žít,\n"
         + "jsou možná hezčí řeky, mají větší třpyt,\n"
         + "však ty jsi moje vlast, má vlast, má věčná matka.";

   final Properties emptyProperties = new Properties();

   @Test
   public void testBasicOperation() throws IOException {
      final Message m1 = new Message();
      m1.setPayload("Ahoj");
      final Message m2 = new Message();
      m2.setPayload("Čau");

      final String dir = TestSetup.createTempDir("PerfCakeDictionaryValidator");
      try {
         // first record some sample data
         DictionaryValidator dv = new DictionaryValidator();
         dv.setDictionaryDirectory(dir);
         dv.setRecord(true);
         Assert.assertTrue(dv.isValid(m1, m2, emptyProperties));
         Assert.assertTrue(dv.isValid(m2, m1, emptyProperties));

         // now check the index cannot be overwritten
         dv = new DictionaryValidator();
         dv.setDictionaryDirectory(dir);
         dv.setRecord(true);
         Assert.assertFalse(dv.isValid(m1, m2, emptyProperties));

         // now verify what we recorded
         dv = new DictionaryValidator();
         dv.setDictionaryDirectory(dir);
         dv.setRecord(false);
         Assert.assertTrue(dv.isValid(m1, m2, emptyProperties));
         Assert.assertTrue(dv.isValid(m2, m1, emptyProperties));
      } finally {
         FileUtils.deleteDirectory(new File(dir));
      }
   }

   @Test
   public void testExtremeMessages() throws IOException {
      final Message m1 = new Message();
      m1.setPayload("Ahoj : = == \\ \\\\ \\= \\: " + SVRATKA + SVRATKA + SVRATKA);
      final Message m2 = new Message();
      m2.setPayload(SVRATKA + SVRATKA + SVRATKA);

      final String dir = TestSetup.createTempDir("PerfCakeDictionaryValidator");
      try {
         // first record some sample data
         DictionaryValidator dv = new DictionaryValidator();
         dv.setDictionaryDirectory(dir);
         dv.setRecord(true);
         Assert.assertTrue(dv.isValid(m1, m2, emptyProperties));
         Assert.assertTrue(dv.isValid(m2, m1, emptyProperties));

         // now verify what we recorded
         dv = new DictionaryValidator();
         dv.setDictionaryDirectory(dir);
         dv.setRecord(false);
         Assert.assertTrue(dv.isValid(m1, m2, emptyProperties));
         Assert.assertTrue(dv.isValid(m2, m1, emptyProperties));
      } finally {
         FileUtils.deleteDirectory(new File(dir));
      }
   }

   @Test
   public void testDuplicateMessage() throws IOException {
      final Message m1 = new Message();
      m1.setPayload("Ahoj");
      final Message m2 = new Message();
      m2.setPayload("Čau");

      final String dir = TestSetup.createTempDir("PerfCakeDictionaryValidator");
      try {
         // first record some sample data
         DictionaryValidator dv = new DictionaryValidator();
         dv.setDictionaryDirectory(dir);
         dv.setRecord(true);
         Assert.assertTrue(dv.isValid(m1, m2, emptyProperties));
         Assert.assertTrue(dv.isValid(m2, m1, emptyProperties));
         Assert.assertFalse(dv.isValid(m1, m2, emptyProperties));

         // now verify what we recorded
         dv = new DictionaryValidator();
         dv.setDictionaryDirectory(dir);
         dv.setRecord(false);
         Assert.assertTrue(dv.isValid(m1, m2, emptyProperties));
         Assert.assertTrue(dv.isValid(m2, m1, emptyProperties));
      } finally {
         FileUtils.deleteDirectory(new File(dir));
      }
   }
}
