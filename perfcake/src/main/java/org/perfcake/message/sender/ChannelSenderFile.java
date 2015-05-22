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
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

/**
 * Sends messages through NIO FileChannel.
 *
 * @author <a href="mailto:domin.hanak@gmail.com">Dominik Hanák</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ChannelSenderFile extends ChannelSender {
   /**
    * Sender's fileChannel
    */
   private FileChannel fileChannel;

   @Override
   public void doInit(final Properties messageAttributes) {
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);

      fileChannel = new RandomAccessFile(getTarget(), "rw").getChannel();

      if (!fileChannel.isOpen()) {
         throw new PerfCakeException("Cannot open a file channel to target: " + getTarget());
      }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      if (messageBuffer != null) {
         fileChannel.write(messageBuffer);

         if (awaitResponse) {
            if (responseBuffer != null) {
               fileChannel.read(responseBuffer);
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
         fileChannel.close();
      } catch (final IOException e) {
         throw new PerfCakeException("Error while closing the file channel: ", e);
      }
   }
}
