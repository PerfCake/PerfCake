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
import org.perfcake.util.ObjectFactory;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.Serializable;
import java.util.Properties;

/**
 * @author Dominik Hanák <domin.hanak@gmail.com>
 * @author Martin Večera <marvenec@gmail.com>
 */
public class ChannelSenderFileTest  {
   private static final String PAYLOAD = "fish";

   @Test
   public void testNormalMessage() throws Exception {
      final Properties senderProperties = new Properties();
      final String file = File.createTempFile("perfcake-", "message.txt").getAbsolutePath();
      senderProperties.setProperty("target", file);
      senderProperties.setProperty("waitResponse", "false");

      final Message message = new Message();
      message.setPayload(PAYLOAD);

      try {
         final ChannelSender sender = (ChannelSenderFile) ObjectFactory.summonInstance(ChannelSenderFile.class.getName(), senderProperties);

         sender.init();
         Assert.assertEquals(sender.getTarget(), file);

         sender.preSend(message, null);
         Assert.assertEquals(sender.getPayload(), PAYLOAD);

         Serializable response = sender.doSend(message, null, null);
         Assert.assertEquals(response, "fish");

         try {
             sender.postSend(message);
         } catch (Exception e) {
             // error while closing, exception thrown - ok
         }

      } catch (Exception e) {
         Assert.fail(e.getMessage(), e.getCause());
      }
   }
}
