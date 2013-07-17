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

package org.perfcake.message.sender;

import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.message.MessageFactory;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidationException;

/**
 * The common ancestor for all senders.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 */
abstract public class AbstractSender implements MessageSender {
   /**
    * The sender's logger.
    */
   private static final Logger log = Logger.getLogger(AbstractSender.class);

   /**
    * The timestamp of a moment when the sending started.
    */
   private long before = -1;

   /**
    * The timestamp of a moment when the response was received.
    */
   private long after = -1;

   /**
    * The sender's target.
    */
   protected String target = "";

   /**
    * Reference to a report manager.
    */
   private ReportManager reportManager;

   /**
    * Reference to a message validator.
    */
   private MessageValidator messageValidator;

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.MessageSender#init()
    */
   @Override
   abstract public void init() throws Exception;

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.MessageSender#close()
    */
   @Override
   abstract public void close() throws PerfCakeException;

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.MessageSender#send(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public final Serializable send(Message message, Map<String, String> properties) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Initializing sending of a message.");
         if (log.isTraceEnabled()) {
            log.trace(String.format("Message content: %s", message.toString()));
         }
      }
      preSend(message, properties);

      if (log.isDebugEnabled()) {
         log.debug("Sending the message.");
      }

      preTime();
      Serializable result = doSend(message, properties);
      postTime();

      if (log.isDebugEnabled()) {
         log.debug("The message has been sent.");
      }

      postSend(message);

      reportResult();
      validateResult(result);

      return result;
   }

   /**
    * @param message
    * @param properties
    * @throws Exception
    */
   abstract public void preSend(Message message, Map<String, String> properties) throws Exception;

   /**
    * Do not use any logger or anything not directly related to sending the
    * message, since this method is being measured
    * 
    * @param message
    * @return
    * @throws Exception
    */
   abstract public Serializable doSend(Message message) throws Exception;

   /**
    * Do not use any logger or anything not directly related to sending the
    * message, since this method is being measured
    * 
    * @param message
    * @param properties
    * @return
    * @throws Exception
    */
   abstract public Serializable doSend(Message message, Map<String, String> properties) throws Exception;

   /**
    * @param message
    * @throws Exception
    */
   abstract public void postSend(Message message) throws Exception;

   /**
    * 
    */
   public void reportResult() {
      if (log.isDebugEnabled()) {
         log.debug("Reporting result.");
      }

      if (reportManager != null) {
         double result = getResponseTime() / 1000000d;

         if (log.isTraceEnabled()) {
            log.trace(String.format("Result is: %f.3", result));
         }

         reportManager.reportResponseTime(result);
      }
   }

   /**
    * @param payload
    * @throws ValidationException
    */
   public void validateResult(Serializable payload) throws ValidationException {
      if (log.isDebugEnabled()) {
         log.debug("Validating result.");
      }

      if (messageValidator != null) {
         Message response = MessageFactory.getMessage(payload);

         if (log.isTraceEnabled()) {
            log.trace(String.format("Response is: %s", response.getPayload().toString()));
         }

         messageValidator.validate(response);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.MessageSender#send(org.perfcake.message.Message)
    */
   @Override
   public final Serializable send(Message message) throws Exception {
      return send(message, null);
   }

   /**
    * 
    */
   private void preTime() {
      before = System.nanoTime();
   }

   /**
    * 
    */
   private void postTime() {
      after = System.nanoTime();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.MessageSender#getResponseTime()
    */
   @Override
   public long getResponseTime() {
      if (after != -1 && before != -1 && after >= before) {
         return after - before;
      } else {
         throw new RuntimeException(toString() + ": Invalid response time: " + before + ">" + after);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.MessageSender#setReportManager(org.perfcake.reporting.ReportManager)
    */
   @Override
   public void setReportManager(ReportManager reportManager) {
      if (log.isDebugEnabled()) {
         log.debug("Setting new report manager.");
      }

      this.reportManager = reportManager;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.MessageSender#setMessageValidator(org.perfcake.validation.MessageValidator)
    */
   @Override
   public void setMessageValidator(MessageValidator messageValidator) {
      if (log.isDebugEnabled()) {
         log.debug("Setting new message validator.");
      }

      this.messageValidator = messageValidator;
   }

   /**
    * Used to read the value of target.
    * 
    * @return The target.
    */
   public String getTarget() {
      return target;
   }

   /**
    * Sets the value of target.
    * 
    * @param target
    *           The target to set.
    */
   public void setTarget(String target) {
      this.target = target;
   }
}
