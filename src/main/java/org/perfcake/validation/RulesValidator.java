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

import org.apache.log4j.Logger;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.perfcake.message.Message;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Validates the message with the defined Drools rules. There is a custom DSL file making the rules specification easier.
 *
 * @author Marek Baluch <baluchw@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class RulesValidator implements MessageValidator {

   private static final Logger log = Logger.getLogger(RulesValidator.class);
   private HashMap<Integer, String> assertions = new HashMap<>();// <lineNo, rule>
   private KieServices ks;
   private KieContainer kc;

   @Override
   public boolean isValid(final Message message) {
      if (ks == null || kc == null) {
         log.error("Rules were not properly loaded.");
         return false;
      }

      KieSession ksess = kc.newKieSession();
      final Map<Integer, String> assertionsCopy = new HashMap<>();
      assertionsCopy.putAll(assertions);

      ksess.setGlobal("rulesUsed", assertionsCopy);
      ksess.insert(message);
      ksess.fireAllRules();
      ksess.dispose();

      for (final Entry<Integer, String> entry : assertionsCopy.entrySet()) {
         if (log.isInfoEnabled()) {
            log.info(String.format("Drools message validation failed with message '%s' and rule '%s'.", message.toString(), entry.getValue()));
         }
      }

      if (!assertionsCopy.isEmpty()) {
         if (log.isInfoEnabled()) {
            log.info(String.format("Drools message validation failed with message '%s' - some rules failed, see previous log for more details.", message.toString()));
         }
         return false;
      }

      return true;
   }

   private void processAssertions(BufferedReader assertionsReader) throws Exception {
      assertions = new HashMap<>();
      int lineNo = 0;
      String line;

      while ((line = assertionsReader.readLine()) != null) {
         line = StringUtil.trim(line);
         if (!"".equals(line) && !line.startsWith("#")) {
            assertions.put(lineNo, line);
            lineNo++;
         }
      }

      ks = KieServices.Factory.get();
      kc = RulesBuilder.build(ks, assertions);
   }

   public void setRules(final String validationRuleFile) {
      try (final FileReader fr = new FileReader(validationRuleFile);
            final BufferedReader br = new BufferedReader(fr)) {
         processAssertions(br);
      } catch (final Exception ex) {
         log.error("Error creating Drools base message validator.", ex);
      }
   }

   public void setRules(final Element validationRule) {
      try (final BufferedReader br = new BufferedReader(new StringReader(validationRule.getTextContent()))) {
         processAssertions(br);
      } catch (final Exception ex) {
         log.error("Error creating Drools base message validator.", ex);
      }
   }

}
