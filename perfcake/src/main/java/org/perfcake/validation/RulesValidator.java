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
import org.perfcake.util.StringUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Validates the message with the defined Drools rules. There is a custom DSL file making the rules specification easier.
 *
 * @author <a href="mailto:baluchw@gmail.com">Marek Baluch</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RulesValidator implements MessageValidator {

   /**
    * Message property key set on the original message for the validator to denote the original message and its response.
    */
   public static final String RULES_ORIGINAL_MESSAGE = "rulesValidator-originalMessage";

   /**
    * A logger for this class.
    */
   private static final Logger log = LogManager.getLogger(RulesValidator.class);

   /**
    * Rules helper that wraps the Drools logic.
    */
   private RulesValidatorHelper rulesValidatorHelper;

   @Override
   public boolean isValid(final Message originalMessage, final Message response, final Properties messageAttributes) {
      if (rulesValidatorHelper == null) {
         log.error("Rules were not properly loaded.");
         return false;
      }

      final Map<Integer, String> unusedAssertions = rulesValidatorHelper.validate(originalMessage, response, messageAttributes);

      for (final Entry<Integer, String> entry : unusedAssertions.entrySet()) {
         if (log.isDebugEnabled()) {
            log.debug(String.format("Drools message validation failed with message '%s' and rule '%s'.", response.toString(), entry.getValue()));
         }
      }

      if (!unusedAssertions.isEmpty()) {
         if (log.isInfoEnabled()) {
            log.info(String.format("Drools message validation failed with message '%s' - some rules failed, see previous log for more details.", response.toString()));
         }
         return false;
      }

      return true;
   }

   /**
    * Creates a new {@link org.perfcake.validation.RulesValidatorHelper} based on the provided assertions.
    *
    * @param assertionsReader
    *       A buffered reader prepared to read the assertions. Each line represents a single assertion.
    * @throws Exception
    *       When it was not possible to read and process the assertions.
    */
   private void processAssertions(final BufferedReader assertionsReader) throws Exception {
      final HashMap<Integer, String> assertions = new HashMap<>();
      int lineNo = 0;
      String line;

      while ((line = assertionsReader.readLine()) != null) {
         line = StringUtil.trim(line);
         if (!"".equals(line) && !line.startsWith("#")) {
            assertions.put(lineNo, line);
            lineNo++;
         }
      }

      rulesValidatorHelper = new RulesValidatorHelper(assertions);
   }

   /**
    * Sets the rules file from which the assertions are loaded.
    *
    * @param validationRuleFile
    *       The file name of the assertions file.
    * @return Instance of this for fluent API.
    */
   public RulesValidator setRules(final String validationRuleFile) {
      try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(validationRuleFile), StandardCharsets.UTF_8))) {
         processAssertions(br);
      } catch (final Exception ex) {
         log.error("Error creating Drools base message validator.", ex);
      }

      return this;
   }

   /**
    * Sets the rules based on an XML element holding the assertions.
    *
    * @param validationRule
    *       The XML element with the assertions.
    */
   public void setRulesAsElement(final Element validationRule) {
      try (final BufferedReader br = new BufferedReader(new StringReader(validationRule.getTextContent()))) {
         processAssertions(br);
      } catch (final Exception ex) {
         log.error("Error creating Drools base message validator.", ex);
      }
   }
}
