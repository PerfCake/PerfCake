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
import org.perfcake.util.Utils;

import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * Invokes external command (specified by {@link #target} property)
 * in a separate process to send the message payload (if message is specified) passed to the standard input of
 * the process or as the command argument.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class CommandSender extends AbstractSender {

   /**
    * Reference to a process where the command is executed.
    */
   private Process process;

   /**
    * The writer that is used to pass the message payload to the command's process standard input stream.
    */
   private PrintWriter writer;

   /**
    * The message payload that is passed to the command to send it.
    */
   private String messagePayload;

   /**
    * The reader that is used to read the response from the command's process standard output stream.
    */
   private InputStreamReader reader;

   /**
    * Specifies from where the message to send is taken.
    */
   private MessageFrom messageFrom = MessageFrom.STDIN;

   /**
    * The prefix for the command.
    */
   private String commandPrefix = "";

   /**
    * The actual command that is executed.
    */
   private String command = "";

   /**
    * The array of environment variables passed to the command.
    */
   private String[] environmentVariables;

   /**
    * The origin where the messages are taken from..
    *
    * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
    * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
    */
   public static enum MessageFrom {
      STDIN, ARGUMENTS
   }

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      // nop
   }

   @Override
   public void doClose() {
      // nop
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);
      if (message != null) {
         final Serializable payload = message.getPayload();
         if (payload != null) {
            messagePayload = payload.toString();
         } else {
            messagePayload = null;
         }
      } else {
         messagePayload = null;
      }

      if (messagePayload != null && messageFrom == MessageFrom.ARGUMENTS) {
         command = (commandPrefix + " " + safeGetTarget(messageAttributes) + " " + messagePayload).trim();
      } else {
         command = (commandPrefix + " " + safeGetTarget(messageAttributes)).trim();
      }

      final Set<Entry<String, String>> propertiesEntrySet = properties.entrySet();
      final Set<Entry<String, String>> envEntrySet = System.getenv().entrySet();
      environmentVariables = new String[propertiesEntrySet.size() + envEntrySet.size() + (message != null ? message.getHeaders().size() + message.getProperties().size() : 0)];
      int i = 0;
      for (final Entry<String, String> entry : propertiesEntrySet) {
         environmentVariables[i++] = entry.getKey() + "=" + entry.getValue();
      }
      for (final Entry<String, String> entry : envEntrySet) {
         environmentVariables[i++] = entry.getKey() + "=" + entry.getValue();
      }
      if (message != null) {
         for (final Entry<Object, Object> entry : message.getHeaders().entrySet()) {
            environmentVariables[i++] = entry.getKey() + "=" + entry.getValue();
         }
         for (final Entry<Object, Object> entry : message.getProperties().entrySet()) {
            environmentVariables[i++] = entry.getKey() + "=" + entry.getValue();
         }
      }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      process = Runtime.getRuntime().exec(command, environmentVariables);
      if (messagePayload != null && messageFrom == MessageFrom.STDIN) {
         writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()), Utils.getDefaultEncoding()), true);
         writer.write(messagePayload);
         writer.flush();
         writer.close();
      }

      process.waitFor();
      final char[] cbuf = new char[10 * 1024];
      this.reader = new InputStreamReader(process.getInputStream(), Utils.getDefaultEncoding());
      // note that Content-Length is available at this point
      final StringBuilder sb = new StringBuilder();
      int ch = reader.read(cbuf);
      while (ch != -1) {
         sb.append(cbuf, 0, ch);
         ch = reader.read(cbuf);
      }

      return sb.toString();
   }

   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);
      reader.close();
      process.getInputStream().close();
   }

   /**
    * Gets the value of messageFrom property.
    *
    * @return The messageFrom value.
    */
   public MessageFrom getMessageFrom() {
      return messageFrom;
   }

   /**
    * Sets the value of messageFrom property.
    *
    * @param messageFrom
    *       The messageFrom value.
    * @return Instance of this to support fluent API.
    */
   public CommandSender setMessageFrom(final MessageFrom messageFrom) {
      this.messageFrom = messageFrom;
      return this;
   }

   /**
    * Gets the value of commandPrefix property value.
    *
    * @return The commandPrefix value.
    */
   protected String getCommandPrefix() {
      return commandPrefix;
   }

   /**
    * Sets the value of commandPrefix property value.
    *
    * @param commandPrefix
    *       The commandPrefix value.
    * @return Instance of this to support fluent API.
    */
   protected CommandSender setCommandPrefix(final String commandPrefix) {
      this.commandPrefix = commandPrefix;
      return this;
   }

   /**
    * Gets an array of environment variables.
    *
    * @return The environment variables array.
    */
   public String[] getEnvironmentVariables() {
      return Arrays.copyOf(environmentVariables, environmentVariables.length); // do not allow external modifications
   }

   /**
    * Sets the environment variables from an array.
    *
    * @param environmentVariables
    *       The environment variables array.
    * @return Instance of this to support fluent API.
    */
   public CommandSender setEnvironmentVariables(final String[] environmentVariables) {
      this.environmentVariables = Arrays.copyOf(environmentVariables, environmentVariables.length); // ignore any later external modifications
      return this;
   }
}
