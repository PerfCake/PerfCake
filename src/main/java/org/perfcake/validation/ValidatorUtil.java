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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities used for messages validation. Used by the rules for {@link org.perfcake.validation.RulesValidator}.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:lucie.fabrikova@gmail.com">Lucie Fabriková</a>
 */
public final class ValidatorUtil {

   /**
    * Log4j logger.
    */
   private static final Logger log = LogManager.getLogger(ValidatorUtil.class);

   /**
    * Non-public constructor to avoid instantiation.
    */
   private ValidatorUtil() {
      super();
   }

   /**
    * Validation operator applied to message part and validated value.
    */
   public enum Operator {
      /**
       * Uses {@link java.lang.String#equals(Object)}.
       */
      EQUALS,

      /**
       * Uses {@link java.lang.String#matches(String)}.
       */
      MATCHES,

      /**
       * Uses {@link java.lang.String#startsWith(String)}.
       */
      STARTS_WITH,

      /**
       * Uses {@link java.lang.String#contains(CharSequence)}.
       */
      CONTAINS,

      /**
       * Uses {@link java.lang.String#endsWith(String)} )}.
       */
      ENDS_WITH,

      /**
       * A part exists.
       */
      EXISTS
   }

   /**
    * Message part that is validated.
    */
   public enum MessagePart {
      /**
       * Message body as a whole.
       */
      BODY,

      /**
       * Message body part.
       */
      BODY_PART, PROPERTY, ATTACHMENT, HEADER_MESSAGE_ID, HEADER_TO, HEADER_FROM, HEADER_REPLY_TO, HEADER_FAULT_TO, HEADER_RELATES_TO, HEADER_ACTION
   }

   /**
    * Message occurrence operator.
    */
   public enum Occurrence {
      /**
       * None such message exists.
       */
      NONE,

      /**
       * At least the specified number of messages exists.
       */
      AT_LEAST,

      /**
       * At most the specified number of messages exists.
       */
      AT_MOST,

      /**
       * Exactly the specified number of messages exists.
       */
      EXACTLY
   }

   /**
    * Validates messages in the <code>list</code> in the interval between <code>from</code> and <code>to</code> borders included. It applies <code>operator</code> on the messages <code>part</code> and
    * the valid <code>value</code>.
    *
    * @param list
    *       Message list.
    * @param from
    *       Begin of the interval.
    * @param to
    *       End of the interval.
    * @param part
    *       Validated message part.
    * @param partValue
    *       Actual value of the parametrized message part (e.g. when <code>part</code> is a body part, this value specifies which
    *       one). If it is not provided, make it <code>null</code>.
    * @param operator
    *       Operator for validation.
    * @param value
    *       Valid value of validated message part.
    * @return A boolean value indicating, if validation of all messages passed
    * (<code>true</code>) or not (<code>false</code>).
    */
   public static boolean validateMessages(final List<Message> list, final int from, final int to, final ValidatorUtil.MessagePart part, final String partValue, final ValidatorUtil.Operator operator, final String value) {
      final int count = list.size();
      if (to <= from) {
         if (log.isErrorEnabled()) {
            log.error("Arguments <from>=" + from + " and <to>=" + to + " are not valid interval borders! <from> should be less than <to>.");
         }
         return false;
      } else if (from < 0 || count < to) {
         if (log.isErrorEnabled()) {
            log.error("Arguments <from>=" + from + " and <to>=" + to + " are out of list's bounds. The list contains " + count + " messages.");
         }
         return false;
      }
      boolean retVal = true;
      for (int i = from; i < to; i++) {
         retVal = retVal && validateMessage(list, i, part, partValue, operator, value);
      }
      return retVal;
   }

   /**
    * Validates message in the <code>list</code> on specified position. It
    * applies <code>operator</code> on the message <code>part</code> and the
    * valid <code>value</code>.
    *
    * @param list
    *       Message list.
    * @param number
    *       Message position in the list.
    * @param part
    *       Validated message part.
    * @param partName
    *       Name of the message part.
    * @param operator
    *       Operator for validation.
    * @param value
    *       Valid value of validated message part.
    * @return A boolean value indicating, if validation of the message passed
    * (<code>true</code>) or not (<code>false</code>).
    */
   public static boolean validateMessage(final List<Message> list, final int number, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value) {
      final int count = list.size();

      if (number < 0 || number >= count) {
         if (log.isErrorEnabled()) {
            log.error("Invalid message position: " + number + " (the list contains " + count + " messages)");
         }
         return false;
      } else {
         return validateMessage((list.get(number)), part, partName, operator, value);
      }
   }

   // TODO check all the cases, do they make sense?
   public static Object getMessagePart(final org.perfcake.message.Message message, final ValidatorUtil.MessagePart part, final String partName) {
      switch (part) {
         case BODY:
            return message.getPayload();
         case PROPERTY:
            return message.getProperty(partName);
         case HEADER_FROM:
            return message.getHeader("from");
         case HEADER_TO:
            return message.getHeader("to");
         case HEADER_REPLY_TO:
            return message.getHeader("replyTo");
         case HEADER_FAULT_TO:
            return message.getHeader("faultTo");
         case HEADER_RELATES_TO:
            return message.getHeader("relatesTo");
         case HEADER_ACTION:
            return message.getHeader("action");
         case ATTACHMENT:
            return null;
         case BODY_PART:
            return null;
         case HEADER_MESSAGE_ID:
            return message.getHeader("messageId");
      }

      if (log.isErrorEnabled()) {
         log.error("This argument values combination (" + part.name() + " + " + partName + ") is not valid for message validation. Please consult with documentation for more information.");
      }

      return null;
   }

   private static boolean validatePart(final String str, final ValidatorUtil.Operator operator, final String value) {
      if (str == null) {
         return false;
      }

      switch (operator) {
         case EQUALS:
            return str.equals(value);
         case MATCHES:
            return str.matches(value);
         case STARTS_WITH:
            return str.startsWith(value);
         case CONTAINS:
            return str.contains(value);
         case ENDS_WITH:
            return str.endsWith(value);
         case EXISTS:
            return true; // now it is evident that the part exists
      }

      if (log.isErrorEnabled()) {
         log.error("This argument values combination (" + operator.name() + " + " + value + ") is not valid for message validation. Please consult with documentation for more information.");
      }

      return false;
   }

   private static String getStringData(final Object data) {
      if (data == null) {
         return null;
      } else {
         if (data instanceof byte[]) {
            try {
               return new String((byte[]) data, Utils.getDefaultEncoding());
            } catch (final UnsupportedEncodingException e) {
               return new String((byte[]) data, Charset.defaultCharset());
            }
         } else {
            return data.toString();
         }
      }
   }

   private static boolean validateData(final Object data, final ValidatorUtil.Operator operator, final String value) {
      final String str = getStringData(data);
      return validatePart(str, operator, value);
   }

   public static boolean validateMessage(final Message message, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value) {
      return validateData(getMessagePart(message, part, partName), operator, value);
   }

   public static boolean validateMessageInverse(final Message message, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value) {
      return !validateMessage(message, part, partName, operator, value);
   }

   /**
    * Validate that the <code>list</code> contains specified number of valid messages.
    *
    * @param list
    *       Message list.
    * @param part
    *       Validated message part.
    * @param partName
    *       Actual value of the parametrized message part (e.g. when <code>part</code> is a body part, this value specifies which
    *       one). If it is not provided, make it <code>null</code>.
    * @param operator
    *       Operator for validation.
    * @param value
    *       Valid value of validated message part.
    * @param occurrence
    *       Type of message occurrence in the <code>list</code>.
    * @param treshold
    *       Treshold for the <code>occurrence</code> metrics.
    * @return A boolean value indicating, if validation of the message passed
    * (<code>true</code>) or not (<code>false</code>) with actual <code>occurrence</code>.
    */
   public static boolean validateMessageOccurance(final List<Message> list, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value, final Occurrence occurrence, final int treshold) {
      switch (occurrence) {
         case NONE:
            for (int i = 0; i < list.size(); i++) {
               if (validateMessage(list, i, part, partName, operator, value)) {
                  return false;
               }
            }
            return true;
         case AT_LEAST:
            return countMessages(list, part, partName, operator, value) >= treshold;
         case AT_MOST:
            return countMessages(list, part, partName, operator, value) <= treshold;
         case EXACTLY:
            return countMessages(list, part, partName, operator, value) == treshold;
      }

      return false;
   }

   /**
    * Validate that the sublist of the <code>list</code> (between <code>from</code> and <code>to</code> (borders included)) contains specified number of valid messages.
    *
    * @param list
    *       Message list.
    * @param from
    *       Begin of the interval.
    * @param to
    *       End of the interval.
    * @param part
    *       Validated message part.
    * @param partName
    *       Actual value of the parametrized message part (e.g. when <code>part</code> is a body part, this value specifies which
    *       one). If it is not provided, make it <code>null</code>.
    * @param operator
    *       Operator for validation.
    * @param value
    *       Valid value of validated message part.
    * @param occurrence
    *       Type of message occurrence in the <code>list</code>.
    * @param treshold
    *       Treshold for the <code>occurrence</code> metrics.
    * @return A boolean value indicating, if validation of the message passed
    * (<code>true</code>) or not (<code>false</code>) with actual <code>occurrence</code>.
    */
   public static boolean validateMessageOccuranceOnInterval(final List<Message> list, final int from, final int to, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value, final Occurrence occurrence, final int treshold) {
      final int count = list.size();
      if (to <= from) {
         if (log.isErrorEnabled()) {
            log.error("Arguments <from>=" + from + " and <to>=" + to + " are not valid interval borders! <from> should be less than <to>.");
         }
         return false;
      } else if (from < 0 || count < to) {
         if (log.isErrorEnabled()) {
            log.error("Arguments <from>=" + from + " and <to>=" + to + " are out of list's bounds. The list contains " + count + " messages.");
         }
         return false;
      }
      final List<Message> subList = new ArrayList<Message>();
      for (int i = from; i <= to; i++) {
         subList.add(list.get(i));
      }
      return validateMessageOccurance(subList, part, partName, operator, value, occurrence, treshold);
   }

   /**
    * Count messages in the list that match the criteria (pass the validation)..
    *
    * @param list
    *       Message list.
    * @param part
    *       Validated message part.
    * @param partName
    *       Actual value of the parametrized message part (e.g. when <code>part</code> is a body part, this value specifies which
    *       one). If it is not provided, make it <code>null</code>.
    * @param operator
    *       Operator for validation.
    * @param value
    *       Valid value of validated message part.
    * @return Number of messages in the list that match the criteria (pass the validation).
    * @see #validateMessage(java.util.List, int, org.perfcake.validation.ValidatorUtil.MessagePart, java.lang.String, org.perfcake.validation.ValidatorUtil.Operator,
    * java.lang.String)
    */
   private static int countMessages(final List<Message> list, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value) {
      int messageCount = 0;
      for (int i = 0; i < list.size(); i++) {
         if (validateMessage(list, i, part, partName, operator, value)) {
            messageCount++;
         }
      }
      return messageCount;
   }
}
