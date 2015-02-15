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
    *
    * @throws java.lang.Exception When anything fails, basically this happens when a connection to the target could not have been established.
    */
   public void init() throws Exception;

   /**
    * Closes the sender.
    *
    * @throws org.perfcake.PerfCakeException When it was not possible to close the target.
    */
   public void close() throws PerfCakeException;

   /**
    * Performs any action that needs to be done to send the message but is not directly related to the sending operation and thus not measured.
    * @param message Message to be sent.
    * @param properties Additional properties that can influence the sending of the message.
    * @throws Exception In case anything fails during the preparation.
    */
   public void preSend(final Message message, final Map<String, String> properties) throws Exception;

   /**
    * Sends a message.
    *
    * @param message Message to be sent.
    * @param mu Measurement unit that carries the current send iteration information.
    *
    * @throws java.lang.Exception When the send operation failed.
    */
   public Serializable send(final Message message, final MeasurementUnit mu) throws Exception;

   /**
    * Sends a message with additional properties.
    *
    * @param message Message to be sent.
    * @param properties Properties that can be used or anyhow influence the sending of the message. Typically carries message headers.
    * @param mu Measurement unit that carries the current send iteration information.
    *
    * @throws java.lang.Exception When the send operation failed.
    */
   public Serializable send(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception;

   /**
    * Performs any action that needs to be done to complete the sending of  the message but is not directly related to the sending operation and thus not measured.
    * @param message Message that was sent.
    * @throws Exception In case anything fails during the finalization.
    */
   public void postSend(final Message message) throws Exception;
}
