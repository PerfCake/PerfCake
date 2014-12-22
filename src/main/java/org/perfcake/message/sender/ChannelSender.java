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


import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * TODO: Provide implementation. This should write to NIO channels.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ChannelSender extends AbstractSender {

   /**
    * Channel target destination.
    */
    private String destination;

   /**
    * UDP or TCP port number
    */
    private int port;

   /**
    * Selector for Datagram or Socket channel
    */
    private Selector selector;
   /**
    * Buffer for writing to and reading from NIO channel.
    */
    private ByteBuffer rwBuffer;

   /**
    * Channel types that are implemented.
    */
    public static enum ChannelType {
        SOCKET, DATAGRAM, FILE
    }

    private ChannelType channelType;

   /**
    * Senders channel
    */
    private Channel sendersChannel;

   /**
    * Message payload
    */
    private String payload;




   @Override
   public void init() throws Exception {
     if (getTarget().contains(":")) {
       String[] parts = target.split(":", 2);
         destination = parts[0];
         port = Integer.valueOf(parts[1]);
       } else {
         destination = getTarget();
       }
   }

   @Override
   public void close() {
       try {
           selector.close();
           sendersChannel.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
   }


   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
       super.preSend(message, properties);
       if (message == null) {
          payload = null;
       } else {
          payload =  message.getPayload().toString();
          CharBuffer c = CharBuffer.wrap(payload);
          Charset charset = Charset.forName("UTF-8");
          rwBuffer = charset.encode(c);
       }

       if (properties.get("channelType") != null) {
           // Set type of NIO channel
           switch (properties.get("channelType")) {
               case  "file" :
                   channelType = ChannelType.FILE;
                   sendersChannel = new RandomAccessFile(destination, "rw").getChannel();
                   break;
               case "datagram" :
                   channelType = ChannelType.DATAGRAM;
                   sendersChannel = DatagramChannel.open();
                   selector = Selector.open();
                   break;
               case "socket" :
                   channelType = ChannelType.SOCKET;
                   sendersChannel = SocketChannel.open();
                   selector = Selector.open();
                   break;
           }
       } else {
           throw new IllegalStateException("Unknown or undefined type of NIO channel. Please use file, datagram or socket.");
       }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      if (payload != null && channelType == ChannelType.FILE) {
         FileChannel fChannel = (FileChannel) sendersChannel;

         fChannel.write(rwBuffer);

         rwBuffer.flip();

         fChannel.read(rwBuffer);

         return rwBuffer.toString();
      }

      if (payload != null && channelType == ChannelType.SOCKET) {
         SocketChannel sChannel = (SocketChannel) sendersChannel;
         sChannel.configureBlocking(false);
         sChannel.connect(new InetSocketAddress(destination, port));

         int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
         SelectionKey key = sChannel.register(selector, interestSet);

         if (key.isWritable()) {
             while(rwBuffer.hasRemaining()) {
                 sChannel.write(rwBuffer);
             }
         }

         rwBuffer.flip();

         if (key.isReadable()) {
             int bytesRead = 0;
             while (bytesRead != -1) {
                 bytesRead = sChannel.read(rwBuffer);
             }
         }

         return rwBuffer.toString();
      }

      if (payload != null && channelType == ChannelType.DATAGRAM) {
         DatagramChannel dChannel = (DatagramChannel) sendersChannel;
         dChannel.configureBlocking(false);
         dChannel.connect(new InetSocketAddress(destination, port));

         int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
         SelectionKey key = dChannel.register(selector, interestSet);

         if (key.isWritable()) {
             while(rwBuffer.hasRemaining()) {
                 dChannel.write(rwBuffer);
             }
         }

         rwBuffer.flip();
         if (key.isReadable()) {
             int bytesRead = 0;
             while (bytesRead != -1) {
                 bytesRead = dChannel.read(rwBuffer);
             }
         }

         return rwBuffer.toString();

      }
      return null;
   }
}
