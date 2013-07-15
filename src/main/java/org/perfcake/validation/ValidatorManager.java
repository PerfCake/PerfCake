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

import java.util.Queue;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.ScenarioExecution;
import org.perfcake.message.Message;
import org.perfcake.message.ReceivedMessage;
import org.w3c.dom.Node;

/**
 * 
 * @author Lucie Fabriková <lucie.fabrikova@gmail.com>
 */
public class ValidatorManager {

   private static TreeMap<String, MessageValidator> validators = new TreeMap<>();

   private static boolean finished = false;

   private static boolean allMessagesValid = true;

   private static boolean enabled = false;

   private static final Logger log = Logger.getLogger(ScenarioExecution.class);

   private static Queue<ReceivedMessage> resultMessages = new FileQueue<ReceivedMessage>("target/messagesQueue");

   public static void setFinished(boolean finished) {
      ValidatorManager.finished = finished;
   }

   public static void addValidator(String validatorId, MessageValidator messageValidator) {
      validators.put(validatorId, messageValidator);
   }

   public static MessageValidator getValidator(String validatorId) {
      return validators.get(validatorId);
   }

   public static void startValidation() {
      Thread t = new Thread(new ValidationThread());
      t.start();
   }

   private static class ValidationThread implements Runnable {

      public ValidationThread() {
      }

      @Override
      public void run() {
         // while test_ended false
         boolean isMessageValid = false;
         ReceivedMessage receivedMessage = null;
         MessageValidator validator = null;

         if (validators.isEmpty()) {
            System.out.println("No validators set in scenario.");
            return;
         }

         receivedMessage = resultMessages.poll();
         while (!finished || receivedMessage != null) {
            while (receivedMessage != null) {
               for (String validatorId : receivedMessage.getSentMessage().getValidatorIdList()) {
                  validator = ValidatorManager.getValidator(validatorId);
                  // verify
                  if (validator != null) {
                     isMessageValid = validator.isValid(new Message(receivedMessage.getPayload()));
                     System.out.println(isMessageValid);
                     allMessagesValid &= isMessageValid;
                  } else {
                     System.out.println("----Validator with id " + receivedMessage.getSentMessage() + " not found.");
                     // message can't be validated
                     allMessagesValid = false;
                  }
                  receivedMessage = resultMessages.poll();
               }
            }
            try {
               Thread.sleep(500);
            } catch (InterruptedException ex) {
               Logger.getLogger(ScenarioExecution.class.getName()).log(Level.ERROR, null, ex);
            }
            receivedMessage = resultMessages.poll();
         }
         if (log.isInfoEnabled()) {
            log.info("ALL MESSAGES VALID: " + allMessagesValid);
         }
      }
   }

   public static void addToResultMessages(ReceivedMessage receivedMessage) {
      resultMessages.add(receivedMessage);
   }

   public static int getSize() {
      return resultMessages.size();
   }

   public static void setAssertionsToValidator(String classname, Node validation, String msgId) {

   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabled(boolean enabled) {
      ValidatorManager.enabled = enabled;
   }

}
