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
import java.nio.file.Files;
import java.util.Properties;

/**
 * Tests {@link org.perfcake.message.sender.ChannelSenderFile}.
 *
 * @author <a href="mailto:domin.hanak@gmail.com">Dominik Hanák</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class ChannelSenderFileTest {
   private static final String PAYLOAD = "fish";
   private static final String originalContent = "xxxxlion";
   private static final String finalContent = "fishlion";

   @Test
   public void testNormalMessage() throws Exception {
      final Properties senderProperties = new Properties();
      final File file = File.createTempFile("perfcake-", "message.txt");
      Files.write(file.toPath(), originalContent.getBytes());
      senderProperties.setProperty("target", file.getAbsolutePath());
      senderProperties.setProperty("awaitResponse", "true");

      final Message message = new Message();
      message.setPayload(PAYLOAD);

      try {
         final ChannelSender sender = (ChannelSenderFile) ObjectFactory.summonInstance(ChannelSenderFile.class.getName(), senderProperties);

         sender.init();
         Assert.assertEquals(sender.getTarget(), file.getAbsolutePath());

         sender.preSend(message, null, null);

         final Serializable response = sender.doSend(message, null, null);
         Assert.assertEquals(response, "lion");

         Assert.assertEquals(new String(Files.readAllBytes(file.toPath())), finalContent);

         sender.postSend(message);
      } catch (final Exception e) {
         Assert.fail(e.getMessage(), e.getCause());
      }
   }
}
