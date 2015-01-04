package org.perfcake.message.sender;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
 *
 * @author Dominik Hanak <domin.hanak@gmail.com>
 */
public class ChannelSenderTest {
    private final String testFile = "message2.txt";
    private final String testPayload = "fish";
    private final String testPort = "4444";

    @Mock
    private InetAddress peerAddress;

    @BeforeMethod
    public void setUp() throws Exception {
       MockitoAnnotations.initMocks(this);
       peerAddress = InetAddress.getLocalHost();
    }

    @Test
    public void testFileChannel() {
        final Properties senderProperties = new Properties();
        senderProperties.setProperty("channelType", "file");
        senderProperties.setProperty("target", testFile);

        final Message message = new Message();
        message.setPayload(testPayload);

        try {
            final ChannelSender sender = (ChannelSender) ObjectFactory.summonInstance(ChannelSender.class.getName(), senderProperties);

            final Map<String, String> additionalMessageProperties = new HashMap<>();
            additionalMessageProperties.put("channelType", "file");

            sender.init();
            Assert.assertEquals(sender.getDestination(), testFile);
            sender.preSend(message, additionalMessageProperties);

            Assert.assertEquals(sender.getTarget(), testFile);
            Assert.assertNotEquals(sender.getChannelType(), ChannelSender.ChannelType.FILE);

            Serializable response = sender.doSend(message, additionalMessageProperties, null);
            Assert.assertEquals(response, "fish");

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e.getCause());
        }
    }



    @Test
    public void testSocketChannel() {
        final Properties senderProperties = new Properties();
        senderProperties.setProperty("channelType", "socket");
        senderProperties.setProperty("target", peerAddress.getHostAddress() + ":" + testPort);

        final Message message = new Message();
        message.setPayload("fish");

        try {
            final ChannelSender sender = (ChannelSender) ObjectFactory.summonInstance(ChannelSender.class.getName(), senderProperties);

            final Map<String, String> additionalMessageProperties = new HashMap<>();
            additionalMessageProperties.put("channelType", "socket");

            sender.init();
            String expTarget = peerAddress.getHostAddress() + ":" + testPort;
            Assert.assertEquals(sender.getTarget(), expTarget);
            sender.preSend(message, additionalMessageProperties);
            Assert.assertNotEquals(sender.getChannelType(), ChannelSender.ChannelType.SOCKET);
            Serializable response = sender.doSend(message, null, null);

            Assert.assertEquals(response, "fish");

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e.getCause());
        }
    }

    @Test
    public void testDatagramChannel() {
        final Properties senderProperties = new Properties();
        senderProperties.setProperty("channelType", "datagram");
        senderProperties.setProperty("target",  peerAddress.getHostAddress() + ":" + testPort);

        try {
            final ChannelSender sender = (ChannelSender) ObjectFactory.summonInstance(ChannelSender.class.getName(), senderProperties);
            final Message message = new Message();
            message.setPayload("fish");

            final Map<String, String> additionalMessageProperties = new HashMap<>();
            additionalMessageProperties.put("channelType", "datagram");

            sender.init();
            String expTarget =  peerAddress.getHostAddress() + ":" + testPort;
            Assert.assertEquals(sender.getTarget(), expTarget);

            sender.preSend(message, additionalMessageProperties);
            Assert.assertNotEquals(sender.getChannelType(), ChannelSender.ChannelType.DATAGRAM);

            Serializable response = sender.doSend(message, additionalMessageProperties, null);

            Assert.assertEquals(response, "fish");

            sender.close();
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e.getCause());
        }
    }
}
