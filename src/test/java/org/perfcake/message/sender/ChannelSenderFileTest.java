package org.perfcake.message.sender;

import org.perfcake.message.Message;
import org.perfcake.util.ObjectFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
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
         Assert.assertEquals(sender.getTarget(), tFILE);

         final Map<String, String> additionalMessageProperties = new HashMap<>();
         additionalMessageProperties.put("waitResponse", "false");

         sender.preSend(message, additionalMessageProperties);
         Assert.assertEquals(sender.getPayload(), tPAYLOAD);

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
