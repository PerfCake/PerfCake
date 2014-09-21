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

import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.util.Utils;

/**
 * The sender that can invoke external command (specified by {@link #target} property)
 * in a separate process to send the message payload (if message is specified) passed to the standard input of
 * the process or as the command argument.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
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
    * The actual command that is executed;
    */
   private String command = "";

   /**
    * The array of environment variables passed to the command.
    */
   private String[] environmentVariables;

   public static enum MessageFrom {
      STDIN, ARGUMENTS;
   }

   @Override
   public void init() throws Exception {
      // nop
   }

   @Override
   public void close() {
      // nop
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties) throws Exception {
      super.preSend(message, properties);
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
         command = (commandPrefix + " " + target + " " + messagePayload).trim();
      } else {
         command = (commandPrefix + " " + target).trim();
      }

      final Set<Entry<String, String>> propertiesEntrySet = properties.entrySet();
      final Set<Entry<String, String>> envEntrySet = System.getenv().entrySet();
      environmentVariables = new String[propertiesEntrySet.size() + envEntrySet.size() + (message != null ? message.getHeaders().size() + message.getProperties().size() : 0)];
      int i = 0;
      for (Entry<String, String> entry : propertiesEntrySet) {
         environmentVariables[i++] = entry.getKey() + "=" + entry.getValue();
      }
      for (Entry<String, String> entry : envEntrySet) {
         environmentVariables[i++] = entry.getKey() + "=" + entry.getValue();
      }
      if (message != null) {
         for (Entry<Object, Object> entry : message.getHeaders().entrySet()) {
            environmentVariables[i++] = entry.getKey() + "=" + entry.getValue();
         }
         for (Entry<Object, Object> entry : message.getProperties().entrySet()) {
            environmentVariables[i++] = entry.getKey() + "=" + entry.getValue();
         }
      }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      process = Runtime.getRuntime().exec(command, environmentVariables);
      if (messagePayload != null && messageFrom == MessageFrom.STDIN) {
         writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()), Utils.getDefaultEncoding()), true);
         writer.write(messagePayload);
         writer.flush();
         writer.close();
      }

      process.waitFor();
      char[] cbuf = new char[10 * 1024];
      this.reader = new InputStreamReader(process.getInputStream(), Utils.getDefaultEncoding());
      // note that Content-Length is available at this point
      StringBuilder sb = new StringBuilder();
      int ch = reader.read(cbuf);
      while (ch != -1) {
         sb.append(cbuf, 0, ch);
         ch = reader.read(cbuf);
      }

      String result = sb.toString();
      return result;
   }

   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);
      reader.close();
      process.getInputStream().close();
   }

   /**
    * Used to read the value of messageFrom property.
    *
    * @return The messageFrom.
    */
   public MessageFrom getMessageFrom() {
      return messageFrom;
   }

   /**
    * Sets the value of messageFrom property.
    *
    * @param messageFrom
    *           The messageFrom to set.
    */
   public CommandSender setMessageFrom(final MessageFrom messageFrom) {
      this.messageFrom = messageFrom;
      return this;
   }

   /**
    * Used to read the value of commandPrefix.
    *
    * @return The commandPrefix.
    */
   protected String getCommandPrefix() {
      return commandPrefix;
   }

   /**
    * Sets the value of commandPrefix.
    *
    * @param commandPrefix
    *           The commandPrefix to set.
    */
   protected CommandSender setCommandPrefix(final String commandPrefix) {
      this.commandPrefix = commandPrefix;
      return this;
   }

   /**
    * @return the environmentVariables
    */
   public String[] getEnvironmentVariables() {
      return Arrays.copyOf(environmentVariables, environmentVariables.length); // do not allow external modifications
   }

   /**
    * @param environmentVariables
    *           the environmentVariables to set
    */
   public CommandSender setEnvironmentVariables(String[] environmentVariables) {
      this.environmentVariables = Arrays.copyOf(environmentVariables, environmentVariables.length); // ignore any later external modifications
      return this;
   }
}
