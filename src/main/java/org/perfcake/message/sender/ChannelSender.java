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
 * @author Dominik Hanák <domin.hanak@gmail.com>
 * @author Martin Večera <marvenec@gmail.com>
 */
abstract public class ChannelSender extends AbstractSender {
   /**
    * The sender's logger.
    */
   protected static final Logger log = Logger.getLogger(ChannelSender.class);

   /**
    * Buffer for writing to and reading from NIO channel.
    */
   protected ByteBuffer rwBuffer = null;

   /**
    * Determines whether we should wait for the response from the channel.
    */
   protected Boolean awaitResponse;

   @Override
   abstract public void init() throws PerfCakeException;

   @Override
   public void close() throws PerfCakeException {
      // no-op
   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      super.preSend(message, properties);
      if (log.isDebugEnabled()) {
         log.debug("Encoding message into buffer.");
      }

      // Encode message payload into buffer
      if (message != null && message.getPayload() != null) {
         CharBuffer c = CharBuffer.wrap(message.getPayload().toString());
         Charset charset = Charset.forName("UTF-8");
         rwBuffer = charset.encode(c);
      } else {
         rwBuffer = null;
      }
   }

   @Override
   public void postSend(Message message) throws Exception {
      super.postSend(message);
   }

   /**
    * Gets the status of waiting for response on the channel.
    *
    * @return True if and only if the sender awaits response.
    */
   public Boolean getAwaitResponse() {
      return awaitResponse;
   }

   /**
    * Specifies whether to wait for a response.
    *
    * @param awaitResponse
    *       True to make the sender to wait for a response.
    */
   public void setAwaitResponse(final Boolean awaitResponse) {
      this.awaitResponse = awaitResponse;
   }
}
