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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.Map;

import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.testng.log4testng.Logger;

/**
 * The common ancestor for all senders that are able to send messages through a socket.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 */
abstract public class AbstractSocketSender extends AbstractSender {

   /**
    * The host of the socket.
    */
   protected String host;

   /**
    * The port of the socket
    */
   protected int port;

   /**
    * The socket for sending.
    */
   protected Socket socket;

   /**
    * A writer for message to be written into the socket.
    */
   private PrintWriter out;

   /**
    * A reader for response to be received from the socket.
    */
   private BufferedReader in;

   /**
    * The sender's logger.
    */
   private Logger log = Logger.getLogger(AbstractSocketSender.class);

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#init()
    */
   @Override
   public void init() throws Exception {
      String[] parts = target.split(":", 2);
      host = parts[0];
      port = Integer.valueOf(parts[1]);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#close()
    */
   @Override
   public void close() {
      // closed per message
   }

   /**
    * Opens a socket on the {@link #host} address.
    * 
    * @throws Exception
    */
   abstract protected void openSocket() throws Exception;

   /**
    * Opens the writer to an outbound socket's stream and the reader to read from inbound socket's stream.
    * 
    * @throws Exception
    */
   private void openStreams() throws Exception {
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
   }

   /**
    * Closes the socket along with the outbound and inbound streams.
    */
   private void closeSocket() {
      out.close();
      try {
         in.close();
      } catch (IOException e) {
         log.warn("Cannot close input stream.", e);
      }
      try {
         socket.close();
      } catch (IOException e) {
         log.warn("Cannot close socket.", e);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#preSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      openSocket();
      openStreams();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message)
    */
   @Override
   public Serializable doSend(Message message) throws Exception {
      out.print(message.getPayload().toString());

      if (out.checkError()) { // flush and check for error
         throw new PerfCakeException(String.format("Error writing to a socket at %s:%d.", host, port));
      }

      StringBuilder sb = new StringBuilder();
      while (in.ready()) {
         sb.append(in.readLine());
      }

      return sb.toString();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public Serializable doSend(Message message, Map<String, String> properties) throws Exception {
      throw new UnsupportedOperationException("This sender does not support message properties.");
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#postSend(org.perfcake.message.Message)
    */
   @Override
   public void postSend(Message message) throws Exception {
      closeSocket();
   }

}
