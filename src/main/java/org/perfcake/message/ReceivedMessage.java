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

/**
 * 
 * @author Lucie Fabriková <lucie.fabrikova@gmail.com>
 */
public class ReceivedMessage implements Serializable {

   private static final long serialVersionUID = 8426248937516343968L;

   /**
    * Received response payload
    */
   private Serializable response;
   /**
    * Original message template
    */
   private MessageTemplate sentMessageTemplate;
   /**
    * Particular message that has been send (placeholders filled with values)
    */
   private Message sentMessage;

   public ReceivedMessage(final Serializable response, final MessageTemplate sentMessageTemplate, final Message sentMessage) {
      this.response = response;
      this.sentMessage = sentMessage;
      this.sentMessageTemplate = sentMessageTemplate;
   }

   public Serializable getResponse() {
      return response;
   }

   public MessageTemplate getSentMessageTemplate() {
      return sentMessageTemplate;
   }

   public Message getSentMessage() {
      return sentMessage;
   }
}
