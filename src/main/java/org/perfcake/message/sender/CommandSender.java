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
 * TODO: Provide implementation. Should invoke external command and pass the message payload as a parameter or stream it to input (configurable).
 * 
 * @author Martin Večeřa <marvec@gmail.com>
 */
public class CommandSender extends AbstractSender {

   // command to be executed
   private String command;

   private Process process;

   private PrintWriter writer;

   private String messagePayload;

   private InputStreamReader read;

   public void setCommand(String command) {
      this.command = command;
   }

   @Override
   public void init() throws Exception {
      this.command = "ls";

   }

   @Override
   public void close() {
      // TODO Auto-generated method stub

   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      this.messagePayload = message.getPayload().toString();
   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties) throws Exception {
      process = Runtime.getRuntime().exec(this.command);

      writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream())), true);
      writer.write(this.messagePayload);
      writer.flush();
      writer.close();

      process.waitFor();

      char[] cbuf = new char[10 * 1024];
      this.read = new InputStreamReader(process.getInputStream());
      // note that Content-Length is available at this point
      StringBuilder sb = new StringBuilder();
      int ch = read.read(cbuf);
      while (ch != -1) {
         sb.append(cbuf, 0, ch);
         ch = read.read(cbuf);
      }

      String result = sb.toString();
      return result;
   }

   @Override
   public Serializable doSend(Message message) throws Exception {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void postSend(Message message) throws Exception {
      read.close();
      process.getInputStream().close();
   }

}
