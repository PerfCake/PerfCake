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
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface for a message sender.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public interface MessageSender {

   /**
    * Initializes the sender with properties. It should be executed after all
    * properties are set.
    */
   public void init() throws Exception;

   /**
    * Closes the sender.
    */
   public void close() throws PerfCakeException;

   public void preSend(final Message message, final Map<String, String> properties) throws Exception;

   /**
    * Sends a message.
    */
   public Serializable send(final Message message, final MeasurementUnit mu) throws Exception;

   /**
    * Sends a message with additional properties.
    */
   public Serializable send(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception;

   public void postSend(final Message message) throws Exception;

}
