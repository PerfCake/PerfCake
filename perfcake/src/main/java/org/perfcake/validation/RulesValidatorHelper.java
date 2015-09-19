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
import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.io.KieResources;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Helps to build KieContainer based on the Drools rules provided. Used by {@link org.perfcake.validation.RulesValidator}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
class RulesValidatorHelper {

   /**
    * A logger for this class.
    */
   static final Logger log = LogManager.getLogger(RulesValidatorHelper.class);

   /**
    * Constant to hold the DSL language file name in the virtual Drools file system.
    */
   private static final String DSL = "messageValidator.dsl";

   /**
    * Assertions checked by the helper.
    */
   private final Map<Integer, String> assertions;

   /**
    * KIE container that holds all Drools related data.
    */
   private final KieContainer kieContainer;

   /**
    * Gets a new helper based on the assertions.
    *
    * @param assertions
    *       Assertions that should be added to the rules.
    * @throws ValidationException
    *       When the KIE container construction fails.
    */
   public RulesValidatorHelper(final Map<Integer, String> assertions) throws ValidationException {
      this.assertions = assertions;

      final KieServices kieServices = KieServices.Factory.get();
      kieContainer = build(kieServices, assertions);
   }

   /**
    * Validates the response given the original message and the previously configured assertions.
    *
    * @param originalMessage
    *       The original message.
    * @param response
    *       The response message.
    * @return Map with unused/invalid assertions.
    */
   public Map<Integer, String> validate(final Message originalMessage, final Message response, final Properties messageAttributes) {
      final KieSession kieSession = kieContainer.newKieSession();
      final Map<Integer, String> unusedAssertions = new HashMap<>();
      unusedAssertions.putAll(assertions);

      kieSession.setGlobal("rulesUsed", unusedAssertions);
      kieSession.setGlobal("messageAttributes", messageAttributes);
      if (originalMessage != null) {
         originalMessage.setProperty(RulesValidator.RULES_ORIGINAL_MESSAGE, "true");
         kieSession.insert(originalMessage);
      }
      if (response != null) {
         kieSession.insert(response);
      }
      kieSession.fireAllRules();
      kieSession.dispose();

      return unusedAssertions;
   }

   /**
    * Build a KIE container from the assertions.
    *
    * @param kieServices
    *       Existing KieServices instance which should be used to build the KIE container.
    * @param assertions
    *       Assertions that represent the rules.
    * @return The new KIE container.
    */
   private KieContainer build(final KieServices kieServices, final Map<Integer, String> assertions) throws ValidationException {
      if (log.isDebugEnabled()) {
         log.debug("Building rules...");
      }

      final StringBuilder sBuilder = new StringBuilder();
      sBuilder.append("package org.perfcake.validation\n\n");
      sBuilder.append("global java.util.Map rulesUsed\n");
      sBuilder.append("global java.util.Properties messageAttributes\n");
      sBuilder.append("import java.util.Map\n");
      sBuilder.append("import java.util.Properties\n");
      sBuilder.append("import org.perfcake.message.Message\n");
      sBuilder.append("import org.perfcake.validation.RulesValidator\n");
      sBuilder.append("import org.perfcake.validation.ValidatorUtil\n");
      sBuilder.append("import org.perfcake.validation.ValidatorUtil.MessagePart\n");
      sBuilder.append("import org.perfcake.validation.ValidatorUtil.Operator\n");
      sBuilder.append("import org.perfcake.validation.ValidatorUtil.Occurrence\n");

      sBuilder.append("expander ").append(DSL).append("\n");

      for (int i = 0; i < assertions.size(); i++) {
         String assertion = assertions.get(i);
         assertion = assertion.trim();
         if (assertion.length() > 0 && !assertion.startsWith("#")) {
            final int lineNumber = i + 1;

            // rules numbered from 1
            final String rule = String.format("rule \"Line %d\"%n  dialect \"java\"%n  when%n    %s%n  then%n   > rulesUsed.remove(%d);%nend%n", lineNumber, assertion, lineNumber - 1);

            sBuilder.append(rule);
         }
      }

      if (log.isTraceEnabled()) {
         log.trace("Built rules:\n" + sBuilder.toString());
      }

      final KieRepository krp = kieServices.getRepository();
      final KieFileSystem kfs = kieServices.newKieFileSystem();
      final KieResources krs = kieServices.getResources();
      final Resource dslResource = krs.newClassPathResource(DSL);
      final Resource rulesResource = krs.newReaderResource(new StringReader(sBuilder.toString()), Utils.getDefaultEncoding());
      kfs.write("src/main/resources/" + DSL, dslResource);
      kfs.write("src/main/resources/org/perfcake/validation/rules.dslr", rulesResource);
      final KieBuilder kb = kieServices.newKieBuilder(kfs);
      kb.buildAll();

      // Check the builder for errors
      if (kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
         if (log.isErrorEnabled()) {
            log.error(kb.getResults().getMessages().toString());
         }
         throw new ValidationException("Unable to compile rules.");
      }

      return kieServices.newKieContainer(krp.getDefaultReleaseId());
   }
}
