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
import org.perfcake.message.Message;
import org.w3c.dom.Element;

/**
 * A validator that checks the message payload for the given regexp.
 *
 * @author Lucie Fabriková <lucie.fabrikova@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class TextValidator implements MessageValidator {

   private static final Logger log = Logger.getLogger(TextValidator.class);

   private String pattern = "";

   @Override
   public boolean isValid(final Message message) {
      final String trimmedLinesOfPayload = StringUtil.trimLines(message.getPayload().toString());
      final String resultPayload = StringUtil.trim(trimmedLinesOfPayload);

      if (!resultPayload.matches(pattern)) {
         if (log.isInfoEnabled()) {
            log.info(String.format("Message payload '%s' does not match the pattern '%s'.", message.getPayload().toString(), pattern));
         }
         return false;
      }

      return true;
   }

   public String getPattern() {
      return pattern;
   }

   public void setPattern(String pattern) {
      this.pattern = pattern;
   }

   public void setPattern(Element pattern) {
      this.pattern = pattern.getTextContent();
   }
}
