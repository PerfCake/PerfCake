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

import org.perfcake.message.ReceivedMessage;

import java.io.Serializable;

/**
 * A single unit of work for validator. This class is immutable.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public final class ValidationTask implements Serializable {

   private static final long serialVersionUID = 4385843103979609134L;

   private final ReceivedMessage receivedMessage;

   private final String threadName;

   /**
    * Creates a new validation task consisting of the response and the thread used to receive the response.
    *
    * @param threadName
    *       The thread name.
    * @param receivedMessage
    *       The received message.
    */
   public ValidationTask(final String threadName, final ReceivedMessage receivedMessage) {
      this.threadName = threadName;
      this.receivedMessage = receivedMessage;
   }

   /**
    * Gets the response in the form of a received message.
    *
    * @return The received message.
    */
   public ReceivedMessage getReceivedMessage() {
      return receivedMessage;
   }

   /**
    * Gets the thread name used to receive the response.
    *
    * @return The thread name.
    */
   public String getThreadName() {
      return threadName;
   }
}
