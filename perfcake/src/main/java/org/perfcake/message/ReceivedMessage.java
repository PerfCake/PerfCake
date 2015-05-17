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
package org.perfcake.message;

import java.io.Serializable;
import java.util.Properties;

/**
 * Holds the couple of sent message and response recorded for it. This is used mainly for validation.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class ReceivedMessage implements Serializable {

   private static final long serialVersionUID = 8426248937516343968L;

   /**
    * Received response payload.
    */
   private final Serializable response;

   /**
    * Original message template.
    */
   private final MessageTemplate sentMessageTemplate;

   /**
    * Snapshot of sequences' values and possible other attributes used for sending a message. These attributes can be used by a validator to replace placeholders.
    */
   private final Properties messageAttributes;

   /**
    * Original message that has been send (placeholders filled with values).
    */
   private final Message sentMessage;

   /**
    * Creates a new received message container.
    *
    * @param response
    *       The received response payload.
    * @param sentMessageTemplate
    *       The original message template.
    * @param sentMessage
    *       The real message sent with the placeholders filled with values.
    * @param messageAttributes
    *       The message payload placeholder values based on the current state of sequences in the {@link org.perfcake.message.sequence.SequenceManager}.
    */
   public ReceivedMessage(final Serializable response, final MessageTemplate sentMessageTemplate, final Message sentMessage, final Properties messageAttributes) {
      this.response = response;
      this.sentMessage = sentMessage;
      this.sentMessageTemplate = sentMessageTemplate;
      this.messageAttributes = messageAttributes;
   }

   /**
    * Gets the stored response.
    *
    * @return The stored response.
    */
   public Serializable getResponse() {
      return response;
   }

   /**
    * Gets the original message template.
    *
    * @return The original message template.
    */
   public MessageTemplate getSentMessageTemplate() {
      return sentMessageTemplate;
   }

   /**
    * Gets the sent message with the placeholders filled with values.
    *
    * @return The sent message with the placeholders filled with values.
    */
   public Message getSentMessage() {
      return sentMessage;
   }

   /**
    * Gets the snapshot of sequence' values and possible other attributes used for sending a message. These attributes can be used by a validator to replace placeholders.
    *
    * @return The snapshot of sequence' values and possible other attributes used for sending a message.
    */
   public Properties getMessageAttributes() {
      return messageAttributes;
   }
}
