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
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.StatefulSession;
import org.drools.rule.Package;
import org.perfcake.message.Message;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Marek Baluch <baluchw@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class RulesMessageValidator implements MessageValidator {

   private static final Logger log = Logger.getLogger(RulesMessageValidator.class);
   private static final String validatorDSL = "messageValidator.dsl";
   private HashMap<Integer, String> assertions = new HashMap<>();// <lineNo, rule>
   private Package pack;

   @Override
   public boolean isValid(final Message message) {
      final RuleBase ruleBase = RuleBaseFactory.newRuleBase();

      ruleBase.addPackage(pack);
      final StatefulSession session = ruleBase.newStatefulSession();

      final Map<Integer, String> assertionsCopy = new HashMap<>();
      assertionsCopy.putAll(assertions);

      session.setGlobal("rulesUsed", assertionsCopy);

      session.insert(message);
      session.fireAllRules();
      session.dispose();

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
      pack = RulesBuilder.build(assertions, validatorDSL);
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
