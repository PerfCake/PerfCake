/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package org.perfcake.scenario;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;

import com.sun.management.HotSpotDiagnosticMXBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "ueber")
public class ScenarioLoaderTest extends TestSetup {

   private static final Logger log = LogManager.getLogger(ScenarioLoaderTest.class);
   private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

   @Test(enabled = false)
   public void testRepeatedLoad() throws PerfCakeException {
      System.gc();
      for (int i = 1; i <= 1000; i++) {
         log.info("***** Cycle no. " + i + " ***** Memory [available/free]: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB/" + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB");
         runScenario("test-repeat");
         System.gc();
      }
      dumpHeap("heap.hprof", true);
   }

   private static void dumpHeap(String fileName, boolean live) {
      try {
         getHotspotMBean().dumpHeap(fileName, live);
      } catch (RuntimeException re) {
         throw re;
      } catch (Exception exp) {
         throw new RuntimeException(exp);
      }
   }

   private static HotSpotDiagnosticMXBean getHotspotMBean() throws IOException {
      final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      final HotSpotDiagnosticMXBean bean = ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
      return bean;
   }

}
