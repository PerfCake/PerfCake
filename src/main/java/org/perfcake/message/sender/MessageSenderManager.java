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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.perfcake.PerfCakeException;
import org.perfcake.reporting.ReportManager;
import org.perfcake.util.ObjectFactory;
import org.perfcake.validation.MessageValidator;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class MessageSenderManager {

   private static final Object syncLock = new Object();
   private int senderPoolSize = 100;
   private String senderClass;
   private final Map<MessageSender, Boolean> messageSendersMap;
   private final Properties messageSenderProperties;
   private Queue<MessageSender> availableSenders;
   protected ReportManager reportManager;
   protected MessageValidator messageValidator;

   public MessageSenderManager() {
      super();
      messageSenderProperties = new Properties();
      availableSenders = new LinkedBlockingQueue<MessageSender>(senderPoolSize);
      messageSendersMap = new ConcurrentHashMap<MessageSender, Boolean>();
   }

   public void setMessageValidator(final MessageValidator messageValidator) {
      this.messageValidator = messageValidator;
   }

   public void setReportManager(final ReportManager reportManager) {
      this.reportManager = reportManager;
   }

   public void setMessageSenderProperty(final String property, final String value) {
      messageSenderProperties.put(property, value);
   }

   public void setMessageSenderProperty(final Object property, final Object value) {
      messageSenderProperties.put(property, value);
   }

   public void init() throws Exception {
      for (int i = 0; i < senderPoolSize; i++) {
         MessageSender sender = (MessageSender) ObjectFactory.summonInstance(senderClass, messageSenderProperties);
         sender.init();
         messageSendersMap.put(sender, false);
         availableSenders.add(sender);
      }
   }

   public synchronized MessageSender acquireSender() throws Exception {
      if (availableSenders.size() > 0) {
         MessageSender messageSender = availableSenders.poll();
         messageSendersMap.put(messageSender, true);
         return messageSender;
      } else {
         throw new PerfCakeException("MessageSender pool is empty.");
      }
   }

   public void releaseSender(final MessageSender messageSender) {
      synchronized (syncLock) {
         if (messageSendersMap.get(messageSender)) {
            availableSenders.add(messageSender);
            messageSendersMap.put(messageSender, false);
         }
      }
   }

   public void releaseAllSenders() {
      synchronized (syncLock) {
         Set<MessageSender> senderSet = messageSendersMap.keySet();
         for (MessageSender sender : senderSet) {
            releaseSender(sender);
         }
      }
   }

   public int availableSenderCount() {
      return availableSenders.size();
   }

   public void close() throws PerfCakeException {
      Iterator<MessageSender> iterator = messageSendersMap.keySet().iterator();
      while (iterator.hasNext()) {
         iterator.next().close();
      }
   }

   public int getSenderPoolSize() {
      return senderPoolSize;
   }

   public void setSenderPoolSize(final int senderPoolSize) {
      this.senderPoolSize = senderPoolSize;
      availableSenders = new LinkedBlockingQueue<MessageSender>(senderPoolSize);
   }

   public String getSenderClass() {
      return senderClass;
   }

   public void setSenderClass(final String senderClass) {
      this.senderClass = senderClass;
   }

   public Map<MessageSender, Boolean> getMessageSendersMap() {
      return messageSendersMap;
   }
}