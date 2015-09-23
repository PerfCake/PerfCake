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
import org.perfcake.util.StringTemplate;
import org.perfcake.util.StringUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the message payload for the given regular expression.
 *
 * It is possible to set the {@link java.util.regex.Pattern#compile(String, int)} compile flags using the particular properties.
 *
 * All flags but {@link java.util.regex.Pattern#UNIX_LINES} are supported. That is because
 * {@link org.perfcake.util.StringUtil#trimLines(String)} is used to pre-process the message payload
 * that changes all line breakers to <code>\n</code>.
 *
 * @author <a href="mailto:lucie.fabrikova@gmail.com">Lucie Fabriková</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @see java.util.regex.Pattern#compile(String, int)
 */
public class RegExpValidator implements MessageValidator {

   private static final Logger log = LogManager.getLogger(RegExpValidator.class);

   private StringTemplate pattern;

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
   public boolean isValid(final Message originalMessage, final Message response, final Properties messageAttributes) {
      final String trimmedLinesOfPayload = StringUtil.trimLines((response == null || response.getPayload() == null) ? "" : response.getPayload().toString());
      final String resultPayload = StringUtil.trim(trimmedLinesOfPayload);

      if (!matches(resultPayload, pattern.toString(messageAttributes))) {
         if (log.isInfoEnabled()) {
            log.info(String.format("Message payload '%s' does not match the pattern '%s'.", (response != null) ? response.getPayload().toString() : "", pattern.toString(messageAttributes)));
         }
         return false;
      }

      return true;
   }

   private boolean matches(final String string, final String regex) {
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

   /**
    * Gets the regular expression pattern.
    *
    * @return The regular expression pattern.
    */
   public String getPattern() {
      return pattern.toString();
   }

   /**
    * Sets the regular expression pattern.
    *
    * @param pattern
    *       The regular expression pattern.
    * @return Instance of this to support fluent API.
    */
   public RegExpValidator setPattern(final String pattern) {
      this.pattern = new StringTemplate(pattern);
      return this;
   }

   /**
    * Sets the regular expression pattern taken from {@link org.w3c.dom.Element Element}'s text content.
    *
    * @param pattern
    *       The DOM element from whose content the regular expression pattern is taken.
    */
   public void setPatternAsElement(final Element pattern) {
      this.pattern = new StringTemplate(pattern.getTextContent());
   }

   /**
    * Gets the value of {@link java.util.regex.Pattern#COMMENTS} flag used to compile the regular expression pattern.
    *
    * @return The value of {@link java.util.regex.Pattern#COMMENTS} flag.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public boolean isComments() {
      return comments;
   }

   /**
    * Sets the value of {@link java.util.regex.Pattern#COMMENTS} flag used to compile the regular expression pattern.
    *
    * @param comments
    *       The value of {@link java.util.regex.Pattern#COMMENTS} flag.
    * @return Instance of this to support fluent API.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public RegExpValidator setComments(final boolean comments) {
      this.comments = comments;
      return this;
   }

   /**
    * Gets the value of {@link java.util.regex.Pattern#CASE_INSENSITIVE} flag used to compile the regular expression pattern.
    *
    * @return The value of {@link java.util.regex.Pattern#CASE_INSENSITIVE} flag.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public boolean isCaseInsensitive() {
      return caseInsensitive;
   }

   /**
    * Sets the value of {@link java.util.regex.Pattern#CASE_INSENSITIVE} flag used to compile the regular expression pattern.
    *
    * @param caseInsensitive
    *       The value of {@link java.util.regex.Pattern#CASE_INSENSITIVE} flag.
    * @return Instance of this to support fluent API.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public RegExpValidator setCaseInsensitive(final boolean caseInsensitive) {
      this.caseInsensitive = caseInsensitive;
      return this;
   }

   /**
    * Gets the value of {@link java.util.regex.Pattern#MULTILINE} flag used to compile the regular expression pattern.
    *
    * @return The value of {@link java.util.regex.Pattern#MULTILINE} flag.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public boolean isMultiline() {
      return multiline;
   }

   /**
    * sets the value of {@link java.util.regex.Pattern#MULTILINE} flag used to compile the regular expression pattern.
    *
    * @param multiline
    *       The value of {@link java.util.regex.Pattern#MULTILINE} flag.
    * @return Instance of this to support fluent API.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public RegExpValidator setMultiline(final boolean multiline) {
      this.multiline = multiline;
      return this;
   }

   /**
    * Gets the value of {@link java.util.regex.Pattern#DOTALL} flag used to compile the regular expression pattern.
    *
    * @return The value of {@link java.util.regex.Pattern#DOTALL} flag.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public boolean isDotall() {
      return dotall;
   }

   /**
    * Sets the value of {@link java.util.regex.Pattern#DOTALL} flag used to compile the regular expression pattern.
    *
    * @param dotall
    *       The value of {@link java.util.regex.Pattern#DOTALL} flag.
    * @return Instance of this to support fluent API.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public RegExpValidator setDotall(final boolean dotall) {
      this.dotall = dotall;
      return this;
   }

   /**
    * Gets the value of {@link java.util.regex.Pattern#UNICODE_CASE} flag used to compile the regular expression pattern.
    *
    * @return The value of {@link java.util.regex.Pattern#UNICODE_CASE} flag.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public boolean isUnicodeCase() {
      return unicodeCase;
   }

   /**
    * Sets the value of {@link java.util.regex.Pattern#UNICODE_CASE} flag used to compile the regular expression pattern.
    *
    * @param unicodeCase
    *       The value of {@link java.util.regex.Pattern#UNICODE_CASE} flag.
    * @return Instance of this to support fluent API.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public RegExpValidator setUnicodeCase(final boolean unicodeCase) {
      this.unicodeCase = unicodeCase;
      return this;
   }

   /**
    * Gets the value of {@link java.util.regex.Pattern#CANON_EQ} flag used to compile the regular expression pattern.
    *
    * @return The value of {@link java.util.regex.Pattern#CANON_EQ} flag.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public boolean isCanonEq() {
      return canonEq;
   }

   /**
    * Sets the value of {@link java.util.regex.Pattern#CANON_EQ} flag used to compile the regular expression pattern.
    *
    * @param canonEq
    *       The value of {@link java.util.regex.Pattern#CANON_EQ} flag.
    * @return Instance of this to support fluent API.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public RegExpValidator setCanonEq(final boolean canonEq) {
      this.canonEq = canonEq;
      return this;
   }

   /**
    * Gets the value of {@link java.util.regex.Pattern#LITERAL} flag used to compile the regular expression pattern.
    *
    * @return The value of {@link java.util.regex.Pattern#LITERAL} flag.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public boolean isLiteral() {
      return literal;
   }

   /**
    * Sets the value of {@link java.util.regex.Pattern#LITERAL} flag used to compile the regular expression pattern.
    *
    * @param literal
    *       The value of {@link java.util.regex.Pattern#LITERAL} flag.
    * @return Instance of this to support fluent API.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public RegExpValidator setLiteral(final boolean literal) {
      this.literal = literal;
      return this;
   }

   /**
    * Gets the value of {@link java.util.regex.Pattern#UNICODE_CHARACTER_CLASS} flag used to compile the regular expression pattern.
    *
    * @return The value of {@link java.util.regex.Pattern#UNICODE_CHARACTER_CLASS} flag.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public boolean isUnicodeCharacterClass() {
      return unicodeCharacterClass;
   }

   /**
    * Sets the value of {@link java.util.regex.Pattern#UNICODE_CHARACTER_CLASS} flag used to compile the regular expression pattern.
    *
    * @param unicodeCharacterClass
    *       The value of {@link java.util.regex.Pattern#UNICODE_CHARACTER_CLASS} flag.
    * @return Instance of this to support fluent API.
    * @see java.util.regex.Pattern#compile(String, int)
    */
   public RegExpValidator setUnicodeCharacterClass(final boolean unicodeCharacterClass) {
      this.unicodeCharacterClass = unicodeCharacterClass;
      return this;
   }
}
