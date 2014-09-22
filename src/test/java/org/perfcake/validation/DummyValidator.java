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

import org.perfcake.message.Message;

/**
 * Test validator that remembers some information when it is called.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class DummyValidator implements MessageValidator {

   /**
    * Last original message passed through the validator.
    */
   private Message lastOriginalMessage;
   /**
    * Last response message passed through the validator.
    */
   private Message lastResponse;
   /**
    * When was the validator called for the last time.
    */
   private long lastCalledTimestamp;
   /**
    * Return value for the validation method.
    */
   private boolean returnValue = true;

   @Override
   public boolean isValid(Message originalMessage, Message response) {
      lastOriginalMessage = originalMessage;
      lastResponse = response;
      lastCalledTimestamp = System.currentTimeMillis();

      return returnValue;
   }

   /**
    * Sets the value the validation method should return.
    *
    * @param returnValue
    *       The value that should be returned from the validation.
    */
   public void setReturnValue(boolean returnValue) {
      this.returnValue = returnValue;
   }

   /**
    * Gets the last original message passed through the validator.
    *
    * @return Last original message passed through the validator.
    */
   public Message getLastOriginalMessage() {

      return lastOriginalMessage;
   }

   /**
    * Gets the last response message passed through the validator.
    *
    * @return Last response message passed through the validator.
    */
   public Message getLastResponse() {
      return lastResponse;
   }

   /**
    * Gets the last timestamp of moment the validation method was called.
    *
    * @return When was the validation method called for the last time.
    */
   public long getLastCalledTimestamp() {
      return lastCalledTimestamp;
   }

   /**
    * Gets the return value for the validation method.
    *
    * @return Return value for the validation method.
    */
   public boolean getReturnValue() {
      return returnValue;
   }
}
