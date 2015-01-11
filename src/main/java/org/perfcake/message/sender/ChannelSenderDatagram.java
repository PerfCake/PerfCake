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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
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
    * Encoded target address.
    */
   private SocketAddress address;

   /**
    * Expected maximum response size. Defaults to -1 which means to instantiate the buffer of the same size as the request messages.
    */
   private int maxResponseSize = -1;

   /**
    * A byte buffer to store the response.
    */
   private ByteBuffer responseBuffer;

   @Override
   public void init() {
      final String[] parts = target.split(":", 2);
      final String host = parts[0];
      final int port = Integer.valueOf(parts[1]);

      address = new InetSocketAddress(host, port);
   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      super.preSend(message, properties);

      // Open the Datagram channel in blocking mode
      datagramChannel = DatagramChannel.open();
      datagramChannel.configureBlocking(true);

      responseBuffer = ByteBuffer.allocate(maxResponseSize < 0 ? rwBuffer.capacity() : maxResponseSize);
   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties, MeasurementUnit mu) throws PerfCakeException {
      if (rwBuffer != null) {
         // write data into channel
         try {
            datagramChannel.send(rwBuffer, address);
         } catch (IOException e) {
            throw new PerfCakeException("Problem while writing to the datagram channel: ", e);
         }

         // flip the buffer so we can read
         rwBuffer.flip();

         // read data from into buffer
         try {
            SocketAddress from = datagramChannel.receive(responseBuffer);
         } catch (IOException e) {
            throw new PerfCakeException("Problem while reading from the datagram channel: ", e);
         }
         return new String(responseBuffer.array(), Charset.forName("UTF-8"));
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

   /**
    * Gets the expected response maximum size. If set to -1, the response buffer will have the same size as the original message.
    *
    * @return The maximum configured buffer size.
    */
   public int getMaxResponseSize() {
      return maxResponseSize;
   }

   /**
    * Sets the expected response maximum size. Set to -1 for the response buffer to have the same size as the original message.
    *
    * @param maxResponseSize
    *       The desired maximum response size.
    */
   public void setMaxResponseSize(final int maxResponseSize) {
      this.maxResponseSize = maxResponseSize;
   }
}
