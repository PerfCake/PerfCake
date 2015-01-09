package org.perfcake.message.sender;

import org.perfcake.message.Message;
import org.perfcake.util.ObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Dominik Hanák <domin.hanak@gmail.com>
 */
public class ChannelSenderDatagramTest {

    private final String tPAYLOAD = "fish";
    private final String tPORT = "4444";

    private InetAddress hostAdress;
    private String target;

    @BeforeMethod
    public void setUp() throws Exception {
        hostAdress = InetAddress.getLocalHost();
        StringBuilder sb = new StringBuilder();
        sb.append(hostAdress.getHostAddress()).append(":").append(tPORT);
        target = sb.toString();
    }

   @Test
   public void testNormalMessage() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", target);

      final Message message = new Message();
      message.setPayload(tPAYLOAD);

      try {
         final ChannelSender sender = (ChannelSenderDatagram) ObjectFactory.summonInstance(ChannelSenderDatagram.class.getName(), senderProperties);

         sender.init();

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

   @Test
   public void testNormalMessageWaitResponse() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", target);

      final Message message = new Message();
      message.setPayload(tPAYLOAD);

      try {
         final ChannelSender sender = (ChannelSenderDatagram) ObjectFactory.summonInstance(ChannelSenderDatagram.class.getName(), senderProperties);

         sender.init();

         final Map<String, String> additionalMessageProperties = new HashMap<>();
         additionalMessageProperties.put("waitResponse", "true");

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

   @Test
   public void testNullMessage() {
      final Properties senderProperties = new Properties();
      senderProperties.setProperty("target", target);

      try {
         final ChannelSender sender = (ChannelSenderDatagram) ObjectFactory.summonInstance(ChannelSenderDatagram.class.getName(), senderProperties);

         sender.init();
         sender.preSend(null, null);
         Assert.assertEquals(sender.getPayload(), null);

         Serializable response = sender.doSend(null, null, null);
         Assert.assertNull(response);

         try {
            sender.postSend(null);
         } catch (Exception e) {
            // error while closing, exception thrown - ok
         }

      } catch (Exception e) {
        Assert.fail(e.getMessage(), e.getCause());
      }
   }

}
