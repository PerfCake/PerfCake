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
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.reporting.reporters;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.perfcake.RunInfo;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.util.ObjectFactory;
import org.perfcake.util.agent.AgentThread;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class MemoryUsageReporterTest {
   private AgentThread agentThread;
   private MemoryUsageReporter mur;
   private final ReportManager rm = new ReportManager();
   private static final long ITERATION_COUNT = 1000l;
   private final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, ITERATION_COUNT));
   private DummyDestination dest;

   private MeasurementUnit mu;

   private static final String AGENT_HOSTNAME = "localhost";
   private static final String AGENT_PORT = "19266";

   @BeforeClass
   private void startPerfCakeAgent() throws InvocationTargetException, ClassNotFoundException, IllegalAccessException, InstantiationException {
      agentThread = new AgentThread("hostname=" + AGENT_HOSTNAME + ",port=" + AGENT_PORT);
      Executors.newSingleThreadExecutor().submit(agentThread);

      final Properties reporterProperties = new Properties();
      reporterProperties.put("agentHostname", AGENT_HOSTNAME);
      reporterProperties.put("agentPort", AGENT_PORT);
      reporterProperties.put("memoryLeakSlopeThreshold", "1"); // 1 byte per second (that should cause positive memory leak detection)
      reporterProperties.put("usedMemoryTimeWindowSize", "5");
      mur = (MemoryUsageReporter) ObjectFactory.summonInstance(MemoryUsageReporter.class.getName(), reporterProperties);

      final Properties destinationProperties = new Properties();
      dest = (DummyDestination) ObjectFactory.summonInstance(DummyDestination.class.getName(), destinationProperties);
   }

   @Test
   public void testMemoryUsageReporterWithMemoryLeakDetection() {
      testMemoryUsageReporter(true);
   }

   @Test
   public void testMemoryUsageReporterWithoutMemoryLeakDetection() {
      testMemoryUsageReporter(false);
   }

   private void testMemoryUsageReporter(boolean memoryLeakDetectionEnabled) {
      Assert.assertNotNull(mur, "Reporter's instance");
      Assert.assertEquals(mur.getAgentHostname(), AGENT_HOSTNAME, "Agent hostname");
      Assert.assertEquals(mur.getAgentPort(), AGENT_PORT, "Agent port");

      final List<Measurement> measurementList = new LinkedList<>();

      mur.setMemoryLeakDetectionEnabled(memoryLeakDetectionEnabled);
      mur.registerDestination(dest, new Period(PeriodType.ITERATION, 100));
      rm.registerReporter(mur);
      rm.setRunInfo(ri);

      rm.start();
      try {
         for (int i = 0; i < ITERATION_COUNT; i++) {
            mu = rm.newMeasurementUnit();
            mu.startMeasure();
            Thread.sleep(1);
            mu.stopMeasure();
            rm.report(mu);
            if (!measurementList.contains(dest.getLastMeasurement())) {
               measurementList.add(dest.getLastMeasurement());
            }
            dest.getLastMeasurement().toString();
         }
      } catch (InterruptedException | ReportingException e) {
         e.printStackTrace();
         Assert.fail("Exception should not be thrown.", e);
      }
      rm.stop();
      final int mls = measurementList.size();
      Assert.assertEquals(mls, 11, "Number of Measurement sent to destination");
      for (final Measurement m : measurementList) {
         Assert.assertNotNull(m.get("Used"), "Used memory result");
         Assert.assertNotNull(m.get("Total"), "Total memory result");
         Assert.assertNotNull(m.get("Max"), "Max memory result");
      }
      final Measurement firstM = measurementList.get(0);
      final Measurement lastM = measurementList.get(mls - 1);
      if (memoryLeakDetectionEnabled) {
         Assert.assertNull(firstM.get("UsedTrend"), "Used memory trend (first measurement)");
         Assert.assertNull(firstM.get("MemoryLeak"), "Memory leak detection (first measurement)");
         Assert.assertNotNull(lastM.get("UsedTrend"), "Used memory trend (last measurement)");
         Assert.assertNotNull(lastM.get("MemoryLeak"), "Memory leak detection (last measurement)");
      } else {
         Assert.assertNull(firstM.get("UsedTrend"), "No used memory trend (first measurement)");
         Assert.assertNull(firstM.get("MemoryLeak"), "No memory leak detection (first measurement)");
         Assert.assertNull(lastM.get("UsedTrend"), "No used memory trend (last measurement)");
         Assert.assertNull(lastM.get("MemoryLeak"), "No memory leak detection (last measurement)");
      }
   }

}
