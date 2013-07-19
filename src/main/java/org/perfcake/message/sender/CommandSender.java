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

import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.perfcake.message.Message;

/**
 * The sender that can invoke external command (specified by {@link #target} property)
 * in a separate process to send the message payload passed to the standard input of
 * the process or as the command argument.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
/**
 * @author pmacik
 * 
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
   
   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#init()
    */
   @Override
   public void init() throws Exception {
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#close()
    */
   @Override
   public void close() {
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#preSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      this.messagePayload = message.getPayload().toString();
      command = (commandPrefix + " " + target + (messageFrom == MessageFrom.ARGUMENTS ? " " + message.getPayload() : "")).trim();

      Set<Entry<String, String>> propertiesEntrySet = properties.entrySet();
      String[] environmentVariables = new String[propertiesEntrySet.size()];
      int i = 0;
      for (Entry<String, String> entry : propertiesEntrySet) {
         environmentVariables[i++] = entry.getKey() + "=" + entry.getValue();
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public Serializable doSend(Message message, Map<String, String> properties) throws Exception {
      process = Runtime.getRuntime().exec(command, environmentVariables);
      if (messageFrom == MessageFrom.STDIN) {
         writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream())), true);
         writer.write(messagePayload);
         writer.flush();
         writer.close();
      }

      process.waitFor();
      char[] cbuf = new char[10 * 1024];
      this.reader = new InputStreamReader(process.getInputStream());
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

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message)
    */
   @Override
   public Serializable doSend(Message message) throws Exception {
      // nop
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#postSend(org.perfcake.message.Message)
    */
   @Override
   public void postSend(Message message) throws Exception {
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
   public void setMessageFrom(MessageFrom messageFrom) {
       this.messageFrom = messageFrom; 
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
   protected void setCommandPrefix(String commandPrefix) {
      this.commandPrefix = commandPrefix;
   }
}
