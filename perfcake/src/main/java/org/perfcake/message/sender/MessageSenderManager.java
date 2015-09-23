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

   /**
    * Number of available senders ready to send the message. Internally configured to reflect number of threads configured in a generator.
    */
   private int senderPoolSize = 100;

   /**
    * Name of the class implementing the sender used in the scenario execution.
    */
   private String senderClass;

   /**
    * Properties of message sender that will be passed to sender instances.
    */
   private final Properties messageSenderProperties = new Properties();

   /**
    * Senders available to send messages.
    */
   private final Queue<MessageSender> availableSenders = new ConcurrentLinkedQueue<>();

   /**
    * All the senders created including those actually busy.
    */
   private final List<MessageSender> allSenders = new ArrayList<>();

   /**
    * Sets a message sender property.
    *
    * @param property
    *       Property name to be set.
    * @param value
    *       A new property string value.
    */
   public void setMessageSenderProperty(final String property, final String value) {
      messageSenderProperties.put(property, value);
   }

   /**
    * Sets a message sender property.
    *
    * @param property
    *       Object referencing the property to be set.
    * @param value
    *       A new property value.
    */
   public void setMessageSenderProperty(final Object property, final Object value) {
      messageSenderProperties.put(property, value);
   }

   /**
    * Copies properties to message sender properties.
    *
    * @param properties
    *       Properties to be added.
    */
   public void addMessageSenderProperties(final Properties properties) {
      if (properties != null) {
         messageSenderProperties.putAll(properties);
      }
   }

   /**
    * Initializes the message sender by creating all the message sender instances.
    *
    * @throws PerfCakeException
    *       When it was not possible to create the instances.
    */
   public void init() throws PerfCakeException {
      availableSenders.clear();
      for (int i = 0; i < senderPoolSize; i++) {

         try {
            final MessageSender sender = (MessageSender) ObjectFactory.summonInstance(senderClass, messageSenderProperties);
            addSenderInstance(sender);
         } catch (Exception e) {
            throw new PerfCakeException("Unable to instantiate sender class: ", e);
         }
      }
   }

   /**
    * Adds {@link MessageSender} into available senders and initializes it.
    *
    * @param sender
    *       Sender to be registered with this manager.
    * @throws PerfCakeException
    *       When the initialization of the sender fails.
    */
   public void addSenderInstance(final MessageSender sender) throws PerfCakeException {
      sender.init();
      availableSenders.add(sender);
      allSenders.add(sender);
   }

   /**
    * Gets a free sender from the pool.
    *
    * @return A sender that is ready to send a message.
    * @throws org.perfcake.PerfCakeException
    *       When the pool is empty.
    */
   public MessageSender acquireSender() throws PerfCakeException {
      final MessageSender ms = availableSenders.poll();
      if (ms != null) {
         return ms;
      } else {
         throw new PerfCakeException("MessageSender pool is empty.");
      }
   }

   /**
    * Returns a sender that has been already used to the pool of available senders for later reuse.
    *
    * @param messageSender
    *       The sender to be returned to the pool.
    */
   public void releaseSender(final MessageSender messageSender) {
      availableSenders.offer(messageSender);
   }

   /**
    * Release all senders that has been acquired previously.
    */
   public void releaseAllSenders() {
      for (final MessageSender ms : allSenders) {
         if (!availableSenders.contains(ms)) {
            availableSenders.offer(ms);
         }
      }
   }

   /**
    * Gets the number of available senders in the pool.
    *
    * @return The number of available senders in the pool.
    */
   public int availableSenderCount() {
      return availableSenders.size();
   }

   /**
    * Finalizes the message sender manager and disconnects all message senders from their target.
    *
    * @throws PerfCakeException
    *       When the disconnection operation failed.
    */
   public void close() throws PerfCakeException {
      for (final MessageSender ms : allSenders) {
         ms.close();
      }
   }

   /**
    * Gets the size of the pool of senders.
    *
    * @return The size of the pool of senders.
    */
   public int getSenderPoolSize() {
      return senderPoolSize;
   }

   /**
    * Sets the size of the pool of senders.
    *
    * @param senderPoolSize
    *       The size of the pool of senders.
    */
   public void setSenderPoolSize(final int senderPoolSize) {
      this.senderPoolSize = senderPoolSize;
   }

   /**
    * Gets the name of the class implementing the message sender.
    *
    * @return The name of the class implementing the message sender.
    */
   public String getSenderClass() {
      return senderClass;
   }

   /**
    * Sets the name of the class implementing the message sender.
    *
    * @param senderClass
    *       The name of the class implementing the message sender.
    */
   public void setSenderClass(final String senderClass) {
      this.senderClass = senderClass;
   }

}
