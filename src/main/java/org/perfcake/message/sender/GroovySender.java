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

package org.perfcake.message.sender;

import java.io.Serializable;
import java.util.Map;

import org.perfcake.message.Message;

/**
 * TODO: Provide implementation. Should invoke external Groovy script and pass the message payload as a parameter or stream it to input (configurable).
 * TODO: This can extend CommandSender probably.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class GroovySender extends CommandSender {

   @Override
   public void init() throws Exception {
      super.setCommand("groovy " + this.address);// groovy groovy-file

   }

   @Override
   public void close() {
      // TODO Auto-generated method stub

   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      super.preSend(message, properties);

   }

   @Override
   public Serializable doSend(Message message) throws Exception {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties) throws Exception {
      Serializable result = super.doSend(message, properties);
      return result;
   }

   @Override
   public void postSend(Message message) throws Exception {
      super.postSend(message);
   }

}
