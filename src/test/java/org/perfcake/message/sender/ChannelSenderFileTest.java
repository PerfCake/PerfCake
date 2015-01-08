package org.perfcake.message.sender;

import org.perfcake.message.Message;
import org.perfcake.util.ObjectFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Properties;

/**
 * @author Dominik Hanák <domin.hanak@gmail.com>
 */
public class ChannelSenderFileTest  {
   private final String tFILE = "message2.txt";
   private final String tPAYLOAD = "fish";

   @Test
   public void testNormalMessage() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", tFILE);

      final Message message = new Message();
      message.setPayload(tPAYLOAD);

      try {
         final ChannelSender sender = (ChannelSenderFile) ObjectFactory.summonInstance(ChannelSenderFile.class.getName(), senderProperties);

         sender.init();
         Assert.assertEquals(sender.getChannelTarget(), tFILE);

         sender.preSend(message, null);
         Assert.assertEquals(sender.getPayload(), tPAYLOAD);

         Serializable response = sender.doSend(message, null, null);
         Assert.assertEquals(response, "fish");

      } catch (Exception e) {
         Assert.fail(e.getMessage(), e.getCause());
      }
   }
}
