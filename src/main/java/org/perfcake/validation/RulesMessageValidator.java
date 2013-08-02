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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.StatefulSession;
import org.drools.rule.Package;
import org.perfcake.message.Message;
import org.w3c.dom.Node;

/**
 * 
 * @author Marek Baluch <baluchw@gmail.com>
 */
public class RulesMessageValidator implements MessageValidator {

   private Package pkg;

   private static final Logger log = Logger.getLogger(RulesMessageValidator.class);

   private final HashMap<String, HashMap<Integer, String>> assertionsMap = new HashMap<>();// 1, <lineNo, rule>

   private final HashMap<String, Package> packagesMap = new HashMap<>();// 1, pkg --> ext: more pkgs to one message

   private static String validatorDSL = "messageValidator.dsl";

   public RulesMessageValidator() {

   }

   @Override
   public void validate(final Message message) throws ValidationException {
      HashMap<Integer, String> assertions;
      final RuleBase ruleBase = RuleBaseFactory.newRuleBase();
      assertions = assertionsMap.get("1");
      pkg = packagesMap.get("1");

      ruleBase.addPackage(pkg);
      final StatefulSession session = ruleBase.newStatefulSession();

      Map<Integer, String> assertionsCopy = new HashMap<>();
      assertionsCopy.putAll(assertions);

      session.setGlobal("rulesUsed", assertionsCopy);

      session.insert(message);
      session.fireAllRules();
      session.dispose();

      for (Integer i : assertionsCopy.keySet()) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error("failed assertion: " + assertionsCopy.get(i));
         }
      }

      if (!assertionsCopy.isEmpty()) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error("Message is not valid");
         }
         throw new ValidationException("Message is not valid: " + message);
      }
   }

   @Override
   public boolean isValid(final Message message) {
      boolean v = true;
      try {
         validate(message);
      } catch (ValidationException e) {
         v = false;
      }
      return v;
   }

   @Override
   public void setAssertions(final Node validationNode, final String msgId) {
      HashMap<Integer, String> assertions = new HashMap<>();
      try {
         BufferedReader br = new BufferedReader(new StringReader(validationNode.getTextContent()));
         int lineNo = 0;
         String line;

         while ((line = br.readLine()) != null) {
            line = StringUtil.trim(line);
            if (!"".equals(line) && !line.startsWith("#")) {
               assertions.put(lineNo, line);
               lineNo++;
            }
         }
         if (this.assertionsMap != null) {
            this.assertionsMap.put(msgId, assertions);
         }
         if (this.packagesMap != null) {
            // build rules - assertions -> pkg
            try {
               pkg = RulesBuilder.build(assertions, validatorDSL);
               this.packagesMap.put("1", pkg);
            } catch (Exception ex) {
               if (log.isEnabledFor(Level.ERROR)) {
                  log.error("Rule building exception, " + ex.getMessage());
               }
            }
         }

      } catch (IOException ex) {
         java.util.logging.Logger.getLogger(RulesMessageValidator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      }
   }
}
