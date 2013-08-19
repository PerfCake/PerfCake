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
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.message.generator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.perfcake.PerfCakeConst;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.ReceivedMessage;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.ValidatorManager;

/**
 * <p>
 * The sender task is a runnable class that is executing a single task of sending the message(s) from the message store using instances of {@link MessageSender} provided by message sender manager (see {@link MessageSenderManager}), receiving the message sender's response and handling the reporting and response message validation.
 * </p>
 * <p>
 * It is used by the generators.
 * </p>
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
class SenderTask implements Runnable {

   /**
    * Reference to a message sender manager that is providing the message senders.
    */
   private MessageSenderManager senderManager;

   /**
    * Reference to a message store where the messages are taken from.
    */
   private List<MessageTemplate> messageStore;

   /**
    * Indicates whether the message numbering is enabled or disabled.
    */
   private boolean messageNumberingEnabled;

   /**
    * Indicates whether the system is in a state when it measures the performance.
    */
   private boolean isMeasuring;

   /**
    * A reference to the current report manager.
    */
   private ReportManager reportManager;

   /**
    * A reference to the current validator manager. It is used to validate message responses.
    */
   private ValidatorManager validatorManager;

   protected SenderTask() {
      // limit the possibilities to construct this class
   }

   private Serializable sendMessage(final MessageSender sender, final Message message, final HashMap<String, String> messageHeaders, final MeasurementUnit mu) throws Exception {
      sender.preSend(message, messageHeaders);

      mu.startMeasure();
      final Serializable result = sender.send(message, messageHeaders, mu);
      mu.stopMeasure();

      sender.postSend(message);

      return result;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Runnable#run()
    */
   @Override
   public void run() {
      assert messageStore != null && reportManager != null && validatorManager != null && senderManager != null : "SenderTask was not properly initialized.";

      final Properties messageAttributes = new Properties();
      final HashMap<String, String> messageHeaders = new HashMap<>();
      MessageSender sender = null;
      ReceivedMessage receivedMessage = null;
      try {
         final MeasurementUnit mu = reportManager.newMeasurementUnit();

         // only set numbering to headers if it is enabled, later there is no change to
         // filter out the headers before sending
         final String msgNumberStr = String.valueOf(mu.getIteration());
         if (messageNumberingEnabled && isMeasuring) {
            messageHeaders.put(PerfCakeConst.MESSAGE_NUMBER_PROPERTY, msgNumberStr);
         }

         final Iterator<MessageTemplate> iterator = messageStore.iterator();
         if (iterator.hasNext()) {
            while (iterator.hasNext()) {

               sender = senderManager.acquireSender();
               final MessageTemplate messageToSend = iterator.next();
               final Message currentMessage = messageToSend.getFilteredMessage(messageAttributes);
               final long multiplicity = messageToSend.getMultiplicity();

               for (int i = 0; i < multiplicity; i++) {
                  receivedMessage = new ReceivedMessage(sendMessage(sender, currentMessage, messageHeaders, mu), messageToSend);
                  if (validatorManager.isEnabled()) {
                     validatorManager.addToResultMessages(receivedMessage);
                  }
               }

               senderManager.releaseSender(sender); // !!! important !!!
               sender = null;
            }
         } else {
            sender = senderManager.acquireSender();
            receivedMessage = new ReceivedMessage(sendMessage(sender, null, messageHeaders, mu), null);
            if (validatorManager.isEnabled()) {
               validatorManager.addToResultMessages(receivedMessage);
            }
            senderManager.releaseSender(sender); // !!! important !!!
            sender = null;
         }

         reportManager.report(mu);
      } catch (final Exception e) {
         e.printStackTrace();
      } finally {
         if (sender != null) {
            senderManager.releaseSender(sender);
         }
      }
   }

   protected void setSenderManager(final MessageSenderManager senderManager) {
      this.senderManager = senderManager;
   }

   protected void setMessageStore(final List<MessageTemplate> messageStore) {
      this.messageStore = messageStore;
   }

   protected void setMessageNumberingEnabled(final boolean messageNumberingEnabled) {
      this.messageNumberingEnabled = messageNumberingEnabled;
   }

   protected void setMeasuring(final boolean isMeasuring) {
      this.isMeasuring = isMeasuring;
   }

   protected void setReportManager(final ReportManager reportManager) {
      this.reportManager = reportManager;
   }

   protected void setValidatorManager(final ValidatorManager validatorManager) {
      this.validatorManager = validatorManager;
   }
}