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
package org.perfcake.message.sender;

import org.perfcake.PerfCakeException;
import org.perfcake.util.ObjectFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages concurrent friendly pool of senders.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MessageSenderManager {

   private int senderPoolSize = 100;
   private String senderClass;
   private final Properties messageSenderProperties = new Properties();
   private Queue<MessageSender> availableSenders = new ConcurrentLinkedQueue<>();
   private List<MessageSender> allSenders = new ArrayList<>();

   public void setMessageSenderProperty(final String property, final String value) {
      messageSenderProperties.put(property, value);
   }

   public void setMessageSenderProperty(final Object property, final Object value) {
      messageSenderProperties.put(property, value);
   }

   public void addMessageSenderProperties(final Properties props) {
      if (props != null) {
         messageSenderProperties.putAll(props);
      }
   }

   public void init() throws Exception {
      availableSenders.clear();
      for (int i = 0; i < senderPoolSize; i++) {
         MessageSender sender = (MessageSender) ObjectFactory.summonInstance(senderClass, messageSenderProperties);
         addSenderInstance(sender);
      }
   }

   /**
    * adds {@link MessageSender} into available senders and initializes it
    *
    * @param sender
    * @throws Exception
    *       if initialization of sender fails
    */
   public void addSenderInstance(MessageSender sender) throws Exception {
      sender.init();
      availableSenders.add(sender);
      allSenders.add(sender);
   }

   public MessageSender acquireSender() throws Exception {
      MessageSender ms = availableSenders.poll();
      if (ms != null) {
         return ms;
      } else {
         throw new PerfCakeException("MessageSender pool is empty.");
      }
   }

   public void releaseSender(final MessageSender messageSender) {
      availableSenders.offer(messageSender);
   }

   public void releaseAllSenders() {
      for (MessageSender ms : allSenders) {
         if (!availableSenders.contains(ms)) {
            availableSenders.offer(ms);
         }
      }
   }

   public int availableSenderCount() {
      return availableSenders.size();
   }

   public void close() throws PerfCakeException {
      for (MessageSender ms : allSenders) {
         ms.close();
      }
   }

   public int getSenderPoolSize() {
      return senderPoolSize;
   }

   public void setSenderPoolSize(final int senderPoolSize) {
      this.senderPoolSize = senderPoolSize;
   }

   public String getSenderClass() {
      return senderClass;
   }

   public void setSenderClass(final String senderClass) {
      this.senderClass = senderClass;
   }

}
