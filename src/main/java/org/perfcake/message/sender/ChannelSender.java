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


import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
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
     * The sender's logger.
     */
    private static final Logger log = Logger.getLogger(ChannelSender.class);

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

    /**
     * Type of NIO channel
     */
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
      if (this.getTarget().contains(":")) {
         String[] parts = target.split(":", 2);
         destination = parts[0];
         port = Integer.valueOf(parts[1]);
      } else {
         destination = getTarget();
      }
   }

   @Override
   public void close() throws PerfCakeException {
      try {
          selector.close();
          sendersChannel.close();
      } catch (IOException e) {
          throw new PerfCakeException("Can't close the channel.");
      }
   }


   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      super.preSend(message, properties);
      log.debug("Encoding the message into buffer.");
      if (message != null) {
         payload =  message.getPayload().toString();
         CharBuffer c = CharBuffer.wrap(payload);
         Charset charset = Charset.forName("UTF-8");
         rwBuffer = charset.encode(c);
      } else {
         payload = null;
      }

      log.debug("Setting channelType.");
      switch (properties.get("channelType")) {
         case "file" :
             this.channelType = ChannelType.FILE;
             sendersChannel = new RandomAccessFile(destination, "rw").getChannel();

             if (!sendersChannel.isOpen()) {
                 throw new PerfCakeException("File was not opened.");
             }
             break;
         case "socket" :
             this.channelType = ChannelType.SOCKET;
             selector = Selector.open();

             // Open the Socket channel in non-blocking mode
             sendersChannel = SocketChannel.open();
             ((SocketChannel) sendersChannel).configureBlocking(false);
             ((SocketChannel) sendersChannel).connect(new InetSocketAddress(destination, port));

             while (!((SocketChannel)sendersChannel).finishConnect()) {
                log.debug("Waiting for channel to finish connection.");
             }

             if (!((SocketChannel) sendersChannel).isConnected()) {
                log.error("Can't connect to target destination.");
                throw new PerfCakeException("Connection to " + getTarget() + " unsuccessful.");
             }
             break;
         case "datagram" :
             this.channelType = ChannelType.DATAGRAM;
             selector = Selector.open();

             // Open the Datagram channel in non-blocking mode
             sendersChannel = DatagramChannel.open();
             ((DatagramChannel) sendersChannel).configureBlocking(false);
             ((DatagramChannel) sendersChannel).connect(new InetSocketAddress(destination, port));

             if (!((DatagramChannel) sendersChannel).isConnected()) {
                 log.error("Can't connect to target destination.");
                 throw new PerfCakeException("Connection to " + getTarget() + " unsuccessful.");
             }
             break;
         default :
             throw new IllegalStateException("Unknown or undefined channel type. Please use file, socket, datagram.");
      }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      if (payload != null && channelType == ChannelType.FILE) {
         FileChannel fChannel = (FileChannel) sendersChannel;

         fChannel.write(rwBuffer);

         rwBuffer.flip();

         fChannel.read(rwBuffer);

         Charset charset = Charset.forName("UTF-8");
         CharBuffer charBuffer = charset.decode(rwBuffer);

         return charBuffer.toString();
      }

      if (payload != null && channelType == ChannelType.SOCKET) {
         SocketChannel sChannel = (SocketChannel) sendersChannel;

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

         Charset charset = Charset.forName("UTF-8");
         CharBuffer charBuffer = charset.decode(rwBuffer);

         return charBuffer.toString();
      }

      if (payload != null && channelType == ChannelType.DATAGRAM) {
         DatagramChannel dChannel = (DatagramChannel) sendersChannel;

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

         Charset charset = Charset.forName("UTF-8");
         CharBuffer charBuffer = charset.decode(rwBuffer);

         return charBuffer.toString();
      }
      return null;
   }

   /**
    * Sets the type of NIO channel
    *
    * @param channelType type of NIO chanel.
    * @return type of channel ChannelSender use.
    *
   */
   public ChannelSender setChannelType(final String channelType) {
       switch (channelType) {
           case "file":
               this.channelType = ChannelType.FILE;
               break;
           case "socket":
               this.channelType = ChannelType.SOCKET;
               break;
           case "datagram":
               this.channelType = ChannelType.DATAGRAM;
               break;
           default:
               throw new IllegalStateException("Unknown or undefined channel type. Please use file, socket, datagram.");
       }

      return this;
   }
}
