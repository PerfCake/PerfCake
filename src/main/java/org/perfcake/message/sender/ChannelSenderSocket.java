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

/**
 * Sender that can send messages through NIO SocketChannel.
 *
 * @author Dominik Hanák <domin.hanak@gmail.com>
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
   public void init() {
      String[] parts = target.split(":", 2);
      host = parts[0];
      port = Integer.valueOf(parts[1]);
   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      super.preSend(message, properties);

      // Open the Socket channel in non-blocking mode
      socketChannel = SocketChannel.open();
      if (waitResponse) {
         // we should wait for response, so open in blocking mode
         socketChannel.configureBlocking(true);
      } else {
         socketChannel.configureBlocking(false);
      }

      try {
         socketChannel.connect(new InetSocketAddress(host, port));
      } catch (UnresolvedAddressException e) {
         throw new PerfCakeException(e.getMessage(), e.getCause());
      }

      while (!socketChannel.finishConnect()) {
         if (log.isDebugEnabled()) {
            log.debug("Waiting for connection to finish.");
         }
      }

      if (!socketChannel.isConnected()) {
         StringBuilder errorMes = new StringBuilder();
         errorMes.append("Connection to ").append(getTarget()).append(" unsuccessful.");

         log.error("Can't connect to target destination.");
         throw new PerfCakeException(errorMes.toString());
      }
   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties, MeasurementUnit mu) throws Exception {
      if (payload != null) {
         // write the message into channel
         try {
            while (rwBuffer.hasRemaining()) {
               socketChannel.write(rwBuffer);
            }
         } catch (IOException e) {
            StringBuilder errorMes = new StringBuilder();
            errorMes.append("Problem while writing into Socket Channel.").append(e.getMessage());

            throw new PerfCakeException(errorMes.toString(), e.getCause());
         }

         // flip the buffer so we can read
         rwBuffer.flip();

         // read the response
         try {
            int bytesRead = socketChannel.read(rwBuffer);
            if (bytesRead == -1) {
               throw new IOException("Host closed the connection or end of stream reached.");
            }
         } catch (IOException e) {
            StringBuilder errorMes = new StringBuilder();
            errorMes.append("Problem while reading from Socket Channel.").append(e.getMessage());

            throw new PerfCakeException(errorMes.toString(), e.getCause());
         }

         return new String(rwBuffer.array(), Charset.forName("UTF-8"));
      }
      return null;
   }

   @Override
   public void postSend(Message message) throws Exception {
      super.postSend(message);
      try {
         socketChannel.close();
      } catch (IOException e) {
         throw new PerfCakeException("Error while closing SocketChannel.", e.getCause());
      }
   }
}
