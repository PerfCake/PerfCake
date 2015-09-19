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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

/**
 * Common ancestor to all sender's sending messages through NIO channels.
 *
 * @author <a href="mailto:domin.hanak@gmail.com">Dominik Hanák</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
abstract public class ChannelSender extends AbstractSender {

   /**
    * Buffer for writing to and reading from NIO channel.
    */
   protected ByteBuffer messageBuffer = null;

   /**
    * Determines whether we should wait for the response from the channel.
    */
   protected boolean awaitResponse;

   /**
    * Expected maximum response size. Defaults to -1 which means to instantiate the buffer of the same size as the request messages.
    */
   protected int maxResponseSize = -1;

   /**
    * A byte buffer to store the response.
    */
   protected ByteBuffer responseBuffer;

   @Override
   public void doClose() throws PerfCakeException {
      // no-op
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);

      // Encode message payload into buffer
      if (message != null && message.getPayload() != null) {
         final CharBuffer c = CharBuffer.wrap(message.getPayload().toString());
         final Charset charset = Charset.forName("UTF-8");
         messageBuffer = charset.encode(c);
      } else {
         messageBuffer = null;
      }

      if (maxResponseSize == -1 && messageBuffer == null) {
         responseBuffer = null;
      } else {
         responseBuffer = ByteBuffer.allocate(maxResponseSize == -1 ? messageBuffer.capacity() : maxResponseSize);
      }
   }

   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);
   }

   /**
    * Gets the status of waiting for response on the channel.
    *
    * @return True if and only if the sender awaits response.
    */
   public boolean getAwaitResponse() {
      return awaitResponse;
   }

   /**
    * Specifies whether to wait for a response.
    *
    * @param awaitResponse
    *       True to make the sender to wait for a response.
    */
   public void setAwaitResponse(final boolean awaitResponse) {
      this.awaitResponse = awaitResponse;
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
