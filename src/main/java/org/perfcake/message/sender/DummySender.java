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

import org.apache.log4j.Logger;
import org.perfcake.message.Message;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvec@gmail.com>
 * 
 */
public class DummySender extends AbstractSender {
   private static final Logger log = Logger.getLogger(DummySender.class);

   @Override
   public void init() throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Initializing... " + address);
      }
      // nop
   }

   @Override
   public void close() {
      if (log.isDebugEnabled()) {
         log.debug("Closing...");
      }
      // nop
   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      // nop
   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Sending to " + address + "...");
      }
      // nop
      return message.getPayload();
   }

   @Override
   public Serializable doSend(Message message) throws Exception {
      return send(message, null);
   }

   @Override
   public void postSend(Message message) throws Exception {
      // nop
   }

}
