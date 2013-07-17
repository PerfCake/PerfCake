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

import org.perfcake.message.Message;

/**
 * The sender that can invoke external command and pass the message payload as a parameter or stream it to input (configurable).
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class CommandSender extends AbstractSender {

   /**
    * Command to be executed.
    */
   private String command = null;

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

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#init()
    */
   @Override
   public void init() throws Exception {
      if (command == null) {
         throw new IllegalArgumentException("The 'command' property is not set");
      }
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
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public Serializable doSend(Message message, Map<String, String> properties) throws Exception {
      process = Runtime.getRuntime().exec(this.command);

      writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream())), true);
      writer.write(this.messagePayload);
      writer.flush();
      writer.close();

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
    * Used to read the value of command.
    * 
    * @return The command.
    */
   public String getCommand() {
      return command;
   }

   /**
    * Sets the value of command.
    * 
    * @param command
    *           The command to set.
    */
   public void setCommand(String command) {
      this.command = command;
   }

}
