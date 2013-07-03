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

import org.perfcake.ObjectWithProperties;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.MessageValidator;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public interface MessageSender extends ObjectWithProperties {

   /**
    * Initializes the sender with properties. It should be executed after all
    * properties are set.
    */
   public void init() throws Exception;

   /** Closes the sender. */
   public void close() throws PerfCakeException;

   /** Sends a message. */
   public Serializable send(Message message) throws Exception;

   /** Sends a message with additional properties. */
   public Serializable send(Message message, Map<String, String> properties) throws Exception;

   /** Returns response time in nanoseconds. */
   public long getResponseTime();

   /** Report manager that any sender can use to report anything **/
   public void setReportManager(ReportManager reportManager);

   /**
    * Message validator usable for I/O (most probably just O) message validation
    */
   public void setMessageValidator(MessageValidator messageValidator);

}
