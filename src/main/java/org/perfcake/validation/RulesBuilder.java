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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.io.KieResources;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.perfcake.util.Utils;

import java.io.StringReader;
import java.util.Map;

/**
 * Helper to build KieContainer based on the Drools rules provided. Used by RulesValidator.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
class RulesBuilder {

   static final Logger log = Logger.getLogger(RulesBuilder.class);
   private static final String DSL = "messageValidator.dsl";

   /**
    * Build KieContainer from the assertions.
    *
    * @param kieServices
    *             Existing KieServices instance which should be used to build KieContainer
    * @param assertions
    *             Assertions that should be added to the rules
    * @return created KieContainer
    */
   public static KieContainer build(final KieServices kieServices, final Map<Integer, String> assertions) throws ValidationException {
      if (log.isDebugEnabled()) {
         log.debug("Building rules...");
      }

      final StringBuilder sBuilder = new StringBuilder();
      sBuilder.append("package org.perfcake.validation\n\n");
      sBuilder.append("global java.util.Map rulesUsed\n");
      sBuilder.append("import java.util.Map\n");
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

      if (log.isDebugEnabled()) {
         log.debug("Built rules:\n" + sBuilder.toString());
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
      if (kb.getResults().hasMessages(Message.Level.ERROR)) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error(kb.getResults().getMessages().toString());
         }
         throw new ValidationException("Unable to compile rules.");
      }

      return kieServices.newKieContainer(krp.getDefaultReleaseId());
   }

}
