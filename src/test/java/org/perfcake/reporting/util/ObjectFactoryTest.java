package org.perfcake.reporting.util;

import java.util.List;
import java.util.Properties;

import org.perfcake.message.generator.ImmediateMessageGenerator;
import org.perfcake.message.sender.CommandSender;
import org.perfcake.message.sender.HTTPSender;
import org.perfcake.util.ObjectFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ObjectFactoryTest {

   @Test
   public void testObjectSummoning() throws Throwable {
      Properties p = new Properties();
      p.setProperty("threads", "42");
      p.setProperty("count", "10001");
      ImmediateMessageGenerator g = (ImmediateMessageGenerator) ObjectFactory.summonInstance("org.perfcake.message.generator.ImmediateMessageGenerator", p);

      Assert.assertEquals(g.getThreads(), 42, "generator's threads");
      Assert.assertEquals(g.getCount(), 10001L, "generator's iteration count");
   }

   @Test
   public void testHTTPSenderSummoning() throws Throwable {
      Properties p = new Properties();
      p.setProperty("method", "GET");
      p.setProperty("target", "http://localhost/get");
      p.setProperty("expectedResponseCodes", "200,203,500");
      HTTPSender sender = (HTTPSender) ObjectFactory.summonInstance("org.perfcake.message.sender.HTTPSender", p);
      Assert.assertEquals(sender.getMethod(), "GET", "HTTP method");
      Assert.assertEquals(sender.getTarget(), "http://localhost/get", "target URL");
      List<Integer> senderExpectedResponseCodeList = sender.getExpectedResponseCodeList();
      Assert.assertEquals(senderExpectedResponseCodeList.size(), 3, "sender's expected response code list size");
      Assert.assertEquals(senderExpectedResponseCodeList.get(0), Integer.valueOf(200), "sender's expected response code");
      Assert.assertEquals(senderExpectedResponseCodeList.get(1), Integer.valueOf(203), "sender's expected response code");
      Assert.assertEquals(senderExpectedResponseCodeList.get(2), Integer.valueOf(500), "sender's expected response code");
   }
   
   @Test
   public void testEnumConverter() throws Throwable {
      Properties p = new Properties();
      p.setProperty("messageFrom", "arguments"); // even lowercase should be accepted
      
      CommandSender sender = (CommandSender) ObjectFactory.summonInstance(CommandSender.class.getName(), p);
      Assert.assertEquals(sender.getMessageFrom(), CommandSender.MessageFrom.ARGUMENTS);

      p.setProperty("messageFrom", "STDIN"); // make sure arguments was not the default value
      
      sender = (CommandSender) ObjectFactory.summonInstance(CommandSender.class.getName(), p);
      Assert.assertEquals(sender.getMessageFrom(), CommandSender.MessageFrom.STDIN);
   }
}
