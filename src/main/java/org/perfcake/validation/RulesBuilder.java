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

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.drools.compiler.PackageBuilder;
import org.drools.rule.Package;

/**
 * 
 * @author Marek Baluch <baluchw@gmail.com>
 * @author Lucie Fabriková <lucie.fabrikova@gmail.com>
 */
public class RulesBuilder {

   static final Logger log = Logger.getLogger(RulesBuilder.class);

   private static final String resourcesDir = "src/main/resources/";

   /**
    * Build scenario assertions into a Drools package.
    * 
    * @param assertions
    *           User assertions (usually defined in a PerfCake scenario file
    * @param dsl
    *           DSL file on java class-path
    * @return compiled Drools package
    * @throws Exception
    */
   public static Package build(final Map<Integer, String> assertions, final String dsl) throws Exception {
      System.out.println("BUILDING RULES");

      if (log.isDebugEnabled()) {
         log.debug("Building rules.");
      }

      final StringBuilder sBuilder = new StringBuilder();
      sBuilder.append("package org.perfcake.validation\n\n");
      sBuilder.append("global java.util.Map rulesUsed\n");
      sBuilder.append("import java.util.Map\n");
      sBuilder.append("import org.perfcake.message.Message\n");
      sBuilder.append("import org.perfcake.validation.ValidatorUtil\n");
      sBuilder.append("import org.perfcake.validation.ValidatorUtil.MessagePart\n");
      sBuilder.append("import org.perfcake.validation.ValidatorUtil.Operator\n");
      sBuilder.append("import org.perfcake.validation.ValidatorUtil.Occurance\n");

      // sBuilder.append("expander messageValidator.dsl\n");
      sBuilder.append("expander ").append(dsl).append("\n");

      for (int i = 0; i < assertions.size(); i++) {
         String assertion = assertions.get(i);
         assertion = assertion.trim();
         if (assertion.length() > 0 && !assertion.startsWith("#")) {
            final int lineNumber = i + 1;

            final String rule = String.format("rule \"Line %d\"\n  dialect \"java\"\n  when\n    %s\n  then\n   > rulesUsed.remove(%d);\nend\n", lineNumber, assertion, lineNumber - 1, lineNumber);// rules
            // numbered
            // from 1

            sBuilder.append(rule);
         }
      }
      if (log.isDebugEnabled()) {
         log.debug("Built rules:\n" + sBuilder.toString());
      }

      // InputStream dslis = RulesBuilder.class.getResourceAsStream(dsl);
      final InputStream dslis = new FileInputStream(resourcesDir + dsl);
      final PackageBuilder pBuilder = new PackageBuilder();
      pBuilder.addPackageFromDrl(new StringReader(sBuilder.toString()), new InputStreamReader(dslis));

      // Check the builder for errors
      if (pBuilder.hasErrors()) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error(pBuilder.getErrors().toString());
         }
         throw new RuntimeException("Unable to compile rules.");
      }

      // get the compiled package (which is serializable)
      final Package pkg = pBuilder.getPackage();
      return pkg;
   }

}
