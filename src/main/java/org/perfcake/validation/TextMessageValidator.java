/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.validation;

import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.message.Message;
import org.w3c.dom.Node;

/**
 * 
 * @author Lucie Fabriková <lucie.fabrikova@gmail.com>
 */
public class TextMessageValidator implements MessageValidator {

   private static final Logger log = Logger.getLogger(TextMessageValidator.class);

   private HashMap<String, String> expectedOutputs = new HashMap<>();// 1, expected output

   // ext: more possible expectedOutputs

   @Override
   public void validate(Message message) throws ValidationException {
      String trimmedLinesOfPayload = StringUtil.trimLines(message.getPayload().toString());
      String resultPayload = StringUtil.trim(trimmedLinesOfPayload);
      String expectedPayload = expectedOutputs.get("1");

      if (!resultPayload.matches(expectedPayload)) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error("Message is not valid.");
            throw new ValidationException("Message is not valid: " + message);
         }
      }
   }

   @Override
   public boolean isValid(Message message) {
      boolean v = true;
      try {
         validate(message);
      } catch (ValidationException e) {
         v = false;
      }
      return v;
   }

   @Override
   public void setAssertions(Node validationNode, String msgId) {
      String expectedOutput = validationNode.getTextContent();

      // refine
      String trimmedLinesOfExpected = StringUtil.trimLines(expectedOutput);
      expectedOutputs.put("1", StringUtil.trim(trimmedLinesOfExpected));
   }

}
