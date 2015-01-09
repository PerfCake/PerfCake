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
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Sender that can send messages through NIO DatagramChannel.
 *
 * @author Lucie Fabriková <lucie.fabrikova@gmail.com>
 * @author Dominik Hanák <domin.hanak@gmail.com>
 */
public class ChannelSenderDatagram extends ChannelSender {

   /**
    * Sender's Datagram Channel
    */
   private DatagramChannel datagramChannel;

   /**
    * TCP or UDP port
    */
   private int port;

   @Override
   public void init() throws Exception {
      if (target != null) {
         String[] parts = target.split(":", 2);
         channelTarget = parts[0];
         port = Integer.valueOf(parts[1]);
      } else {
         throw new IllegalStateException("Target not set. Please set the target property.");
      }
   }

   @Override
   public void close() throws PerfCakeException {
     // no
   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      super.preSend(message, properties);

      // Open the Datagram channel in non-blocking mode
      datagramChannel = DatagramChannel.open();
      datagramChannel.configureBlocking(false);
      datagramChannel.connect(new InetSocketAddress(channelTarget, port));

      if (!datagramChannel.isConnected()) {
         StringBuilder errorMes = new StringBuilder();
         errorMes.append("Connection to ").append(getTarget()).append(" unsuccessful.");

         log.error("Can't connect to target destination.");
         throw new PerfCakeException(errorMes.toString());
      }

   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties, MeasurementUnit mu) throws Exception {
      if (payload != null) {
         // write data into channel
         try {
            while (rwBuffer.hasRemaining()) {
               datagramChannel.write(rwBuffer);
            }
         } catch (IOException e) {
            StringBuilder errorMes = new StringBuilder();
            errorMes.append("Problem while writing into Datagram Channel.").append(e.getMessage());

            throw new PerfCakeException(errorMes.toString(), e.getCause());
         }

         // flip the buffer so we can read
         rwBuffer.flip();

         // read data from into buffer
         try {
            datagramChannel.read(rwBuffer);
         } catch (IOException e) {
            StringBuilder errorMes = new StringBuilder();
            errorMes.append("Problem while reading from Datagram Channel.").append(e.getMessage());

            throw new PerfCakeException(errorMes.toString());
         }

         Charset charset = Charset.forName("UTF-8");
         CharBuffer charBuffer = charset.decode(rwBuffer);

         return charBuffer.toString();
      }

      return null;
   }

   @Override
   public void postSend(Message message) throws Exception {
      super.postSend(message);
      try {
         datagramChannel.close();
      } catch (IOException e) {
         throw new PerfCakeException("Error while closing DatagramChannel.", e.getCause());
      }
   }
}
