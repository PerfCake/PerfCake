package org.perfcake.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.reporting.reporters.Reporter;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ObjectFactoryTest {
   
   @BeforeClass
   public void prepareScenarioParser() throws PerfCakeException, URISyntaxException, IOException {
      System.setProperty(PerfCakeConst.PLUGINS_DIR_PROPERTY, getClass().getResource("/plugins").getPath());
   }

   @Test
   public void testLoadPluginClass() {
      Object testReporter = null;
      try {
         testReporter = ObjectFactory.summonInstance("org.perfcake.plugins.test_reporter.TestReporter", new Properties());
      } catch(Exception e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
      Assert.assertNotNull(testReporter);
      Assert.assertTrue(testReporter instanceof Reporter, String.format("Class %s doesn't seem to be instance of Reporter", testReporter.getClass().getName()));
   }
   
   @Test(expectedExceptions = ClassNotFoundException.class)
   public void testNonExistingPluginDirClass() throws Exception {
      System.setProperty(PerfCakeConst.PLUGINS_DIR_PROPERTY, "hchkrdtn");
      @SuppressWarnings("unused") Object testReporter = ObjectFactory.summonInstance("org.perfcake.plugins.test_reporter.TestReporter", new Properties());
   }
}
