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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A validator that checks the message payload for the given regexp.
 *
 * It is possible to set the {@link java.util.regex.Pattern#compile(String, int)} compile flags using the particular properties.
 *
 * All flags but {@link java.util.regex.Pattern#UNIX_LINES} are supported. That is because
 * {@link org.perfcake.util.StringUtil#trimLines(String)} is used to pre-process the message payload
 * that changes all line breakers to <code>\n</code>.
 *
 * @author Lucie Fabriková <lucie.fabrikova@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @see java.util.regex.Pattern#compile(String, int)
 */
public class RegExpValidator implements MessageValidator {

   private static final Logger log = LogManager.getLogger(RegExpValidator.class);

   private String pattern = "";

   // java.util.regex.Pattern flags
   private boolean caseInsensitive = false;
   private boolean multiline = false;
   private boolean dotall = false;
   private boolean unicodeCase = false;
   private boolean canonEq = false;
   private boolean literal = false;
   private boolean unicodeCharacterClass = false;
   private boolean comments = false;

   @Override
   public boolean isValid(final Message originalMessage, final Message response) {
      final String trimmedLinesOfPayload = StringUtil.trimLines(response == null ? "" : response.getPayload().toString());
      final String resultPayload = StringUtil.trim(trimmedLinesOfPayload);

      if (!matches(resultPayload, pattern)) {
         if (log.isInfoEnabled()) {
            log.info(String.format("Message payload '%s' does not match the pattern '%s'.", response.getPayload().toString(), pattern));
         }
         return false;
      }

      return true;
   }

   private boolean matches(String string, String regex) {
      int flags = 0;
      if (caseInsensitive) {
         flags = flags | Pattern.CASE_INSENSITIVE;
      }
      if (multiline) {
         flags = flags | Pattern.MULTILINE;
      }
      if (dotall) {
         flags = flags | Pattern.DOTALL;
      }
      if (unicodeCase) {
         flags = flags | Pattern.UNICODE_CASE;
      }
      if (canonEq) {
         flags = flags | Pattern.CANON_EQ;
      }
      if (literal) {
         flags = flags | Pattern.LITERAL;
      }
      if (unicodeCharacterClass) {
         flags = flags | Pattern.UNICODE_CHARACTER_CLASS;
      }
      if (comments) {
         flags = flags | Pattern.COMMENTS;
      }
      final Pattern p = Pattern.compile(regex, flags);
      final Matcher m = p.matcher(string);
      return m.matches();
   }

   public String getPattern() {
      return pattern;
   }

   public RegExpValidator setPattern(String pattern) {
      this.pattern = pattern;
      return this;
   }

   public void setPatternAsElement(Element pattern) {
      this.pattern = pattern.getTextContent();
   }

   public boolean isComments() {
      return comments;
   }

   public RegExpValidator setComments(final boolean comments) {
      this.comments = comments;
      return this;
   }

   public boolean isCaseInsensitive() {
      return caseInsensitive;
   }

   public RegExpValidator setCaseInsensitive(final boolean caseInsensitive) {
      this.caseInsensitive = caseInsensitive;
      return this;
   }

   public boolean isMultiline() {
      return multiline;
   }

   public RegExpValidator setMultiline(final boolean multiline) {
      this.multiline = multiline;
      return this;
   }

   public boolean isDotall() {
      return dotall;
   }

   public RegExpValidator setDotall(final boolean dotall) {
      this.dotall = dotall;
      return this;
   }

   public boolean isUnicodeCase() {
      return unicodeCase;
   }

   public RegExpValidator setUnicodeCase(final boolean unicodeCase) {
      this.unicodeCase = unicodeCase;
      return this;
   }

   public boolean isCanonEq() {
      return canonEq;
   }

   public RegExpValidator setCanonEq(final boolean canonEq) {
      this.canonEq = canonEq;
      return this;
   }

   public boolean isLiteral() {
      return literal;
   }

   public RegExpValidator setLiteral(final boolean literal) {
      this.literal = literal;
      return this;
   }

   public boolean isUnicodeCharacterClass() {
      return unicodeCharacterClass;
   }

   public RegExpValidator setUnicodeCharacterClass(final boolean unicodeCharacterClass) {
      this.unicodeCharacterClass = unicodeCharacterClass;
      return this;
   }
}
