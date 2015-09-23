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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

/**
 * Sends messages through NIO SocketChannel.
 *
 * @author <a href="mailto:domin.hanak@gmail.com">Dominik Hanák</a>
 */
public class ChannelSenderSocket extends ChannelSender {

   /**
    * Sender's SocketChannel.
    */
   private SocketChannel socketChannel;

   /**
    * TCP or UDP port.
    */
   private int port;

   /**
    * Host address.
    */
   private String host;

   @Override
   public void doInit(final Properties messageAttributes) {
      final String[] parts = safeGetTarget(messageAttributes).split(":", 2);
      host = parts[0];
      port = Integer.parseInt(parts[1]);
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);

      // Open the Socket channel in non-blocking mode
      socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(true);

      try {
         socketChannel.connect(new InetSocketAddress(host, port));
      } catch (final UnresolvedAddressException e) {
         throw new PerfCakeException("Cannot connect to the socket channel: ", e);
      }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      if (messageBuffer != null) {
         // write the message into channel
         try {
            while (messageBuffer.hasRemaining()) {
               socketChannel.write(messageBuffer);
            }
         } catch (final IOException e) {
            throw new PerfCakeException("Problem while writing to the socket channel: ", e);
         }

         // read the response
         if (awaitResponse) {
            if (responseBuffer != null) {
               try {
                  socketChannel.read(responseBuffer);
               } catch (final IOException e) {
                  throw new PerfCakeException("Problem while reading from the socket channel: ", e);
               }

               return new String(responseBuffer.array(), Charset.forName("UTF-8"));
            } else {
               throw new PerfCakeException("Cannot read response with automatic buffer size configuration for an empty message.");
            }
         }
      }
      return null;
   }

   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);
      try {
         socketChannel.close();
      } catch (final IOException e) {
         throw new PerfCakeException("Error while closing the socket channel: ", e);
      }
   }
}
