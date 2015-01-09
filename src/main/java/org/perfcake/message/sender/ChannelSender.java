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

import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Common ancestor to all sender's sending messages through NIO channels.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Dominik Hanák <domin.hanak@gmail.com>
 */
abstract public class ChannelSender extends AbstractSender {

   /**
    * The sender's logger.
    */
   protected static final Logger log = Logger.getLogger(ChannelSender.class);

   /**
    * Buffer for writing to and reading from NIO channel.
    */
   protected ByteBuffer rwBuffer;

   /**
    * Message payload
    */
   protected String payload;

   /**
    *  Target of channel.
    *  Not the same as {@link #target} when working with Socket or DatagramChannel.
    */
   protected String channelTarget;


   @Override
   abstract public void init() throws Exception;

   @Override
   abstract  public void close() throws PerfCakeException;


   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      super.preSend(message, properties);
      if (log.isDebugEnabled()) {
          log.debug("Encoding message into buffer.");
      }

      // Encode message payload into buffer
      if (message != null && message.getPayload() != null) {
         payload = message.getPayload().toString();
         CharBuffer c = CharBuffer.wrap(payload);
         Charset charset = Charset.forName("UTF-8");
         rwBuffer = charset.encode(c);
      } else {
         payload = null;
      }
   }

   @Override
   public void postSend(Message message) throws Exception {
      super.postSend(message);
   }

    /**
    * Sets the channelTarget for data sending.
    *
    * @param channelTarget {@link #target}
    * @return ChannelSender with new target.
    */
   public ChannelSender setChannelTarget(final String channelTarget) {
      this.channelTarget = channelTarget;
      return this;
   }

    /**
     * Sets the payload attribute
     *
     * @param payload message payload
     * @return ChannelSender with new payload
     */
   public ChannelSender setPayload(final String payload) {
      this.payload = payload;
      return this;
   }

   /**
    * Returns channelTarget.
    * @return channelTarget
    */
   public String getChannelTarget() {
       return this.channelTarget;
   }

   /**
    * Returns payload of message
    * @return payload
    */
   public String getPayload() {
      return this.payload;
   }
}
