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
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Sender that can send messages through NIO DatagramChannel.
 *
 * @author Dominik Hanák <domin.hanak@gmail.com>
 * @author Martin Večera <marvenec@gmail.com>
 */
public class ChannelSenderDatagram extends ChannelSender {

   /**
    * Sender's Datagram Channel.
    */
   private DatagramChannel datagramChannel;

   /**
    * TCP or UDP port.
    */
   private int port;

   /**
    * Host address.
    */
   private String host;

   @Override
   public void init() {
      String[] parts = target.split(":", 2);
      host = parts[0];
      port = Integer.valueOf(parts[1]);
   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      super.preSend(message, properties);

      // Open the Datagram channel in non-blocking mode
      datagramChannel = DatagramChannel.open();
      datagramChannel.configureBlocking(true);

      try {
         datagramChannel.connect(new InetSocketAddress(host, port));
      } catch (Exception e) {
         throw new PerfCakeException("Exception thrown when connecting to target.");
      }

      if (!datagramChannel.isConnected()) {
         log.error("Can't connect to target destination.");
         throw new PerfCakeException("Connection to " + getTarget() + " unsuccessful.");
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
            throw new PerfCakeException("Problem while writing into Datagram Channel: ", e);
         }

         // flip the buffer so we can read
         rwBuffer.flip();

         // read data from into buffer
         try {
            int bytesRead = datagramChannel.read(rwBuffer);
            if (bytesRead == -1) {
               throw new IOException("Host closed the connection or end of stream reached.");
            }
         } catch (IOException e) {
            throw new PerfCakeException("Problem while reading from Datagram Channel: ", e);
         }
         return new String(rwBuffer.array(), Charset.forName("UTF-8"));
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
