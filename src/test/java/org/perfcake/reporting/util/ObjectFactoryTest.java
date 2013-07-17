package org.perfcake.reporting.util;

import java.util.Properties;

import junit.framework.Assert;

import org.perfcake.message.generator.ImmediateMessageGenerator;
import org.perfcake.message.sender.HTTPSender;
import org.perfcake.util.ObjectFactory;
import org.testng.annotations.Test;

public class ObjectFactoryTest {

   @Test
   public void testObjectSummoning() throws Throwable {
      Properties p = new Properties();
      p.setProperty("threads", "42");
      p.setProperty("count", "10001");
      ImmediateMessageGenerator g = (ImmediateMessageGenerator) ObjectFactory.summonInstance("org.perfcake.message.generator.ImmediateMessageGenerator", p);

      Assert.assertEquals(42, g.getThreads());
      Assert.assertEquals(10001L, g.getCount());

   }

   @Test
   public void testHTTPSenderSummoning() throws Throwable {
      Properties p = new Properties();
      p.setProperty("method", "GET");
      p.setProperty("target", "http://localhost/get");
      HTTPSender sender = (HTTPSender) ObjectFactory.summonInstance("org.perfcake.message.sender.HTTPSender", p);
      Assert.assertEquals("GET", sender.getMethod());
      Assert.assertEquals("http://localhost/get", sender.getTarget());
   }
}
