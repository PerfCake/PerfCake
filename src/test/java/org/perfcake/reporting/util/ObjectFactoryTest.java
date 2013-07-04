package org.perfcake.reporting.util;

import java.util.Properties;

import junit.framework.Assert;

import org.perfcake.message.generator.ImmediateMessageGenerator;
import org.perfcake.util.ObjectFactory;
import org.testng.annotations.Test;

public class ObjectFactoryTest {

   @Test
   public void testObjectSummoning() throws Throwable {
      Properties p = new Properties();
      p.setProperty("threads", "42");
      p.setProperty("measureResponseTime", "true");
      p.setProperty("timeWindowSize", "123");
      p.setProperty("count", "10001");
      ImmediateMessageGenerator g = (ImmediateMessageGenerator) ObjectFactory.summonInstance("org.perfcake.message.generator.ImmediateMessageGenerator", p);

      Assert.assertEquals(42, g.getThreads());
      Assert.assertEquals(true, g.isMeasureResponseTime());
      Assert.assertEquals(123, g.getTimeWindowSize());
      Assert.assertEquals(10001L, g.getCount());

   }
}
