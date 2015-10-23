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
import java.util.Properties;

/**
 * A contract of message sender. The ultimate goal of a message sender is to send a message (or any other unit of communication work),
 * and possibly receive a response. Any implementation should not do anything but the communication. It should be a pure wrapper of
 * the message exchange layer.
 *
 * {@link #init()} and {@link #close()} methods should be used to establish and close a permanent connection.
 * It is a design consideration of any implementation whether to handle the connection establishment separately (and not measure it),
 * or to open and close a connection with every single request (and make it part of the performance measurement).
 * Most provided implementations (if not all) handle the connection separately as we are really interested only in measuring the message
 * exchange.
 *
 * {@link #preSend} and {@link #postSend} methods are still not part of the performance measurement and can prepare the message for
 * actual sending or handle any cleanup.
 *
 * {@link #send} methods must handle just the message exchange. No logging or complex error handling code should be placed here. Therefore
 * we allow any generic exception to be thrown.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public interface MessageSender {

   /**
    * Initializes the sender. This method is executed once the sender is properly created based on the configuration in the scenario.
    *
    * @throws PerfCakeException
    *       When anything fails, basically this happens when a connection to the target could not have been established.
    */
   void init() throws PerfCakeException;

   /**
    * Closes the sender.
    *
    * @throws org.perfcake.PerfCakeException
    *       When it was not possible to close the target.
    */
   void close() throws PerfCakeException;

   /**
    * Performs any action that needs to be done to send the message but is not directly related to the sending operation and thus not measured.
    *
    * @param message
    *       Message to be sent.
    * @param properties
    *       Additional properties that can influence the sending of the message.
    * @param messageAttributes
    *       Attributes that can be used to replace placeholders in message and or target.
    * @throws Exception
    *       In case anything fails during the preparation.
    */
   void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception;

   /**
    * Sends a message.
    *
    * @param message
    *       Message to be sent.
    * @param measurementUnit
    *       Measurement unit that carries the current send iteration information.
    * @return Received response.
    * @throws java.lang.Exception
    *       When the send operation failed.
    */
   Serializable send(final Message message, final MeasurementUnit measurementUnit) throws Exception;

   /**
    * Sends a message with additional properties.
    *
    * @param message
    *       Message to be sent.
    * @param properties
    *       Properties that can be used or anyhow influence the sending of the message. Typically carries message headers.
    * @param measurementUnit
    *       Measurement unit that carries the current send iteration information.
    * @return Received response.
    * @throws java.lang.Exception
    *       When the send operation failed.
    */
   Serializable send(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception;

   /**
    * Performs any action that needs to be done to complete the sending of  the message but is not directly related to the sending operation and thus not measured.
    *
    * @param message
    *       Message that was sent.
    * @throws Exception
    *       In case anything fails during the finalization.
    */
   void postSend(final Message message) throws Exception;

   /**
    * Gets the target where to send the messages.
    *
    * @return The current target.
    */
   String getTarget();

   /**
    * Gets the target where to send the messages providing additional properties to replace placeholders in the
    * tearget template.
    *
    * @param properties
    *       Additional properties to replace placeholders in the target template.
    * @return The current target.
    */
   String getTarget(final Properties properties);

   /**
    * Sets the target where to send the messages.
    *
    * @param target
    *       The target to be set.
    * @return Instance of this for fluent API.
    */
   MessageSender setTarget(final String target);
}
