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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.message.Message;

/**
 * Utility used for validating messages.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>, Lucie Fabriková <lucie.fabrikova@gmail.com>
 */
public final class ValidatorUtil {

   /**
    * log4j logger.
    */
   private static final Logger log = Logger.getLogger(ValidatorUtil.class);

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
      EQUALS, MATCHES, STARTS_WITH, CONTAINS, ENDS_WITH, EXISTS
   }

   /**
    * Message part that is validated.
    */
   public enum MessagePart {
      BODY, BODY_PART, PROPERTY, ATTACHMENT, HEADER_MESSAGE_ID, HEADER_TO, HEADER_FROM, HEADER_REPLY_TO, HEADER_FAULT_TO, HEADER_RELATES_TO, HEADER_ACTION
   }

   /**
    * Message occurance operator.
    */
   public enum Occurance {
      NONE, AT_LEAST, AT_MOST, EXACTLY
   }

   /**
    * Validates messages in the <code>list</code> in the interval between <code>from</code> and <code>to</code> borders included. It applies <code>operator</code> on the messages <code>part</code> and
    * the valid <code>value</code>.
    * 
    * @param list
    *           Message list.
    * @param from
    *           Begin of the interval.
    * @param to
    *           End of the interval.
    * @param part
    *           Validated message part.
    * @param partValue
    *           Actual value of the parametrized message part (e.g. when <code>part</code> is a body part, this value specifies which
    *           one). If it is not provided, make it <code>null</code>.
    * @param operator
    *           Operator for validation.
    * @param value
    *           Valid value of validated message part.
    * @return A boolean value indicating, if validation of all messages passed
    *         (<code>true</code>) or not (<code>false</code>).
    */
   public static boolean validateMessages(final List<Message> list, final int from, final int to, final ValidatorUtil.MessagePart part, final String partValue, final ValidatorUtil.Operator operator, final String value) {
      int count = list.size();
      if (to <= from) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error("Arguments <from>=" + from + " and <to>=" + to + " are not valid interval borders! <from> should be less than <to>.");
         }
         return false;
      } else if (from < 0 || count < to) {
         if (log.isEnabledFor(Level.ERROR)) {
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
    *           Message list.
    * @param number
    *           Message position in the list.
    * @param part
    *           Validated message part.
    * @param partValue
    *           Actual value of the parametrized message part (e.g. when <code>part</code> is a body part, this value specifies which
    *           one). If it is not provided, make it <code>null</code>.
    * @param operator
    *           Operator for validation.
    * @param value
    *           Valid value of validated message part.
    * @return A boolean value indicating, if validation of the message passed
    *         (<code>true</code>) or not (<code>false</code>).
    */
   public static boolean validateMessage(final List<Message> list, final int number, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value) {
      int count = list.size();

      if (number < 0 || number >= count) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error("Invalid message position: " + number + " (the list contains " + count + " messages)");
         }
         return false;
      } else {
         return validateMessage((list.get(number)), part, partName, operator, value);
      }
   }

   // private static Object getMessagePart(Message message, MessagePart part, String partName) {
   // switch (part) {
   // case BODY:
   // return message.getBody().get();
   // case BODY_PART:
   // return message.getBody().get(partName);
   // case PROPERTY:
   // return message.getProperties().getProperty(partName);
   // case ATTACHMENT:
   // return message.getAttachment().get(partName);
   // case HEADER_FROM:
   // return message.getHeader().getCall().getFrom().getAddr();
   // case HEADER_TO:
   // return message.getHeader().getCall().getTo().getAddr();
   // case HEADER_REPLY_TO:
   // return message.getHeader().getCall().getReplyTo().getAddr();
   // case HEADER_FAULT_TO:
   // return message.getHeader().getCall().getFaultTo().getAddr();
   // case HEADER_RELATES_TO:
   // return message.getHeader().getCall().getRelatesTo();
   // case HEADER_ACTION:
   // return message.getHeader().getCall().getAction();
   // case HEADER_MESSAGE_ID:
   // return message.getHeader().getCall().getMessageID();
   // }
   //
   // if (log.isEnabledFor(Level.ERROR)) {
   // log.error("This argument values combination (" + part.name() + " + " + partName + ") is not valid for message validation. Please consult with documentation for more information.");
   // }
   //
   // return null;
   // }

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
      }

      if (log.isEnabledFor(Level.ERROR)) {
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

      if (log.isEnabledFor(Level.ERROR)) {
         log.error("This argument values combination (" + operator.name() + " + " + value + ") is not valid for message validation. Please consult with documentation for more information.");
      }

      return false;
   }

   private static String getStringData(final Object data) {
      if (data == null) {
         return null;
      } else {
         if (data instanceof byte[]) {
            return new String((byte[]) data);
         } else {
            return data.toString();
         }
      }
   }

   private static boolean validateData(final Object data, final ValidatorUtil.Operator operator, final String value) {
      String str = getStringData(data);
      return validatePart(str, operator, value);
   }

   // public static boolean validateMessage(org.perfcake.message.Message message, MessagePart part, String partName, Operator operator, String value) {
   // return validateData(getMessagePart(message, part, partName), operator, value);
   // }

   public static boolean validateMessage(final Message message, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value) {
      return validateData(getMessagePart(message, part, partName), operator, value);
   }

   // public static boolean validateMessageInverse(org.jboss.soa.esb.qa.perfcake.message.Message message, MessagePart part, String partName, Operator operator, String value) {
   // return !validateMessage(message, part, partName, operator, value);
   // }
   public static boolean validateMessageInverse(final Message message, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value) {
      return !validateMessage(message, part, partName, operator, value);
   }

   // /**
   // * Validate if message ID of the first message in the <code>list</code> on
   // * specified position is equal to message ID of the second one.
   // *
   // * @param list
   // * Message list.
   // * @param number1
   // * Position of the first message.
   // * @param number2
   // * Position of the second message.
   // * @return A boolean value telling, if message IDs of the messages are equal
   // * (<code>true</code>) or not (<code>false</code>).
   // */
   // public static boolean validateMessageIdEquals(List<Message> list, int number1, int number2) {
   // int count = list.size();
   // if (number1 < 0 || number1 >= count) {
   // log.error("Invalid message #1 position: " + number1 + " (the list contains " + count + " messages)");
   // return false;
   // } else if (number2 < 0 || number2 >= count) {
   // log.error("Invalid message #2 position: " + number2 + " (the list contains " + count + " messages)");
   // return false;
   // } else {
   // return ((Message) list.get(number1)).getHeader().getCall().getMessageID().toString().equals(((Message) list.get(number2)).getHeader().getCall().getMessageID().toString());
   // }
   // }

   /**
    * Validate that the <code>list</code> contains specified number of valid messages.
    * 
    * @param list
    *           Message list.
    * @param part
    *           Validated message part.
    * @param partName
    *           Actual value of the parametrized message part (e.g. when <code>part</code> is a body part, this value specifies which
    *           one). If it is not provided, make it <code>null</code>.
    * @param operator
    *           Operator for validation.
    * @param value
    *           Valid value of validated message part.
    * @param occurance
    *           Type of message occurance in the <code>list</code>.
    * @param treshold
    *           Treshold for the <code>occurance</code> metrics.
    * @return A boolean value indicating, if validation of the message passed
    *         (<code>true</code>) or not (<code>false</code>) with actual <code>occurance</code>.
    */
   public static boolean validateMessageOccurance(final List<Message> list, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value, final ValidatorUtil.Occurance occurance, final int treshold) {
      switch (occurance) {
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
    *           Message list.
    * @param from
    *           Begin of the interval.
    * @param to
    *           End of the interval.
    * @param part
    *           Validated message part.
    * @param partName
    *           Actual value of the parametrized message part (e.g. when <code>part</code> is a body part, this value specifies which
    *           one). If it is not provided, make it <code>null</code>.
    * @param operator
    *           Operator for validation.
    * @param value
    *           Valid value of validated message part.
    * @param occurance
    *           Type of message occurance in the <code>list</code>.
    * @param treshold
    *           Treshold for the <code>occurance</code> metrics.
    * @return A boolean value indicating, if validation of the message passed
    *         (<code>true</code>) or not (<code>false</code>) with actual <code>occurance</code>.
    */
   public static boolean validateMessageOccuranceOnInterval(final List<Message> list, final int from, final int to, final ValidatorUtil.MessagePart part, final String partName, final ValidatorUtil.Operator operator, final String value, final ValidatorUtil.Occurance occurance, final int treshold) {
      int count = list.size();
      if (to <= from) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error("Arguments <from>=" + from + " and <to>=" + to + " are not valid interval borders! <from> should be less than <to>.");
         }
         return false;
      } else if (from < 0 || count < to) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error("Arguments <from>=" + from + " and <to>=" + to + " are out of list's bounds. The list contains " + count + " messages.");
         }
         return false;
      }
      List<Message> subList = new ArrayList<Message>();
      for (int i = from; i <= to; i++) {
         subList.add(list.get(i));
      }
      return validateMessageOccurance(subList, part, partName, operator, value, occurance, treshold);
   }

   /**
    * Count messages in the list that match the criteria (pass the validation)..
    * 
    * @param list
    *           Message list.
    * @param part
    *           Validated message part.
    * @param partName
    *           Actual value of the parametrized message part (e.g. when <code>part</code> is a body part, this value specifies which
    *           one). If it is not provided, make it <code>null</code>.
    * @param operator
    *           Operator for validation.
    * @param value
    *           Valid value of validated message part.
    * @return Number of messages in the list that match the criteria (pass the validation).
    * @see #validateMessage(java.util.List, int, org.jboss.soa.esb.qa.perfcake.validation.ValidatorUtil.MessagePart, java.lang.String, org.jboss.soa.esb.qa.perfcake.validation.ValidatorUtil.Operator,
    *      java.lang.String)
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

   // public static boolean measurePerformance(List<Message> list) {
   // String property;
   // long start = 0, stop = 0, count = 0;
   //
   // for (Message message : list) {
   // if ((property = (String) message.getProperties().getProperty(PerfCakeConst.PERFORMANCE_MESSAGE_PROPERTY)) != null) {
   // if (property.equals(PerfCakeConst.START_VALUE)) {
   // start = (Long) message.getProperties().getProperty(PerfCakeConst.TIME_MESSAGE_PROPERTY);
   // } else if (property.equals(PerfCakeConst.STOP_VALUE)) {
   // stop = (Long) message.getProperties().getProperty(PerfCakeConst.TIME_MESSAGE_PROPERTY);
   // count = (Long) message.getProperties().getProperty(PerfCakeConst.COUNT_MESSAGE_PROPERTY);
   // } else if (property.equals(PerfCakeConst.CHECK_POINT_VALUE)) {
   // }
   // }
   // }
   //
   // //report
   // if (log.isInfoEnabled()) {
   // log.info(count + " messages was processed in " + (stop - start) + " ms (" + 1000f * count / (stop - start) + " msg/s)");
   // }
   // return true;
   // }
}
