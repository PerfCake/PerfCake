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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;

/**
 * The common ancestor for all senders that are able to send messages through a socket.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
abstract public class AbstractSocketSender extends AbstractSender {

   /**
    * The host of the socket.
    */
   protected String host;

   /**
    * The port of the socket.
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
   private final Logger log = LogManager.getLogger(AbstractSocketSender.class);

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      final String[] parts = safeGetTarget(messageAttributes).split(":", 2);
      host = parts[0];
      port = Integer.parseInt(parts[1]);
   }

   @Override
   public void doClose() {
      // closed per message
   }

   /**
    * Opens a socket on the {@link #host} address.
    *
    * @throws Exception
    *       When it was not possible to open the socket.
    */
   abstract protected void openSocket() throws Exception;

   /**
    * Opens the writer to an outbound socket's stream and the reader to read from inbound socket's stream.
    *
    * @throws Exception
    *       When it was not possible to open socket streams.
    */
   private void openStreams() throws Exception {
      out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Utils.getDefaultEncoding()), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Utils.getDefaultEncoding()));
   }

   /**
    * Closes the socket along with the outbound and inbound streams.
    */
   private void closeSocket() {
      out.close();
      try {
         in.close();
      } catch (final IOException e) {
         log.warn("Cannot close input stream.", e);
      }
      try {
         socket.close();
      } catch (final IOException e) {
         log.warn("Cannot close socket.", e);
      }
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);
      openSocket();
      openStreams();
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      out.print(message.getPayload().toString());

      if (out.checkError()) { // flush and check for error
         throw new PerfCakeException(String.format("Error writing to a socket at %s:%d.", host, port));
      }

      final StringBuilder sb = new StringBuilder();
      while (in.ready()) {
         sb.append(in.readLine());
      }

      return sb.toString();
   }

   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);
      closeSocket();
   }

}
