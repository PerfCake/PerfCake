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

package org.perfcake.message;


/**
 * 
 * @author Lucie Fabriková <lucie.fabrikova@gmail.com>
 */
public class MessageToSend {

   private Message message;

   private long multiplicity;

   private String validatorId;// may be null

   public MessageToSend() {

   }

   public MessageToSend(Message message, long multiplicity, String validatorId) {
      this.message = message;
      this.multiplicity = multiplicity;
      this.validatorId = validatorId;
   }

   public Message getMessage() {
      return message;
   }

   public void setMessage(Message message) {
      this.message = message;
   }

   public Long getMultiplicity() {
      return multiplicity;
   }

   public void setMultiplicity(Long multiplicity) {
      this.multiplicity = multiplicity;
   }

   public String getValidatorId() {
      return validatorId;
   }

   public void setValidatorId(String validatorId) {
      this.validatorId = validatorId;
   }

}
