/*
 * Copyright 2010-2013 the original author or authors.
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
import org.perfcake.util.agent.PerfCakeAgent.Memory;
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
   private final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));
   private DummyDestination dest;

   private MeasurementUnit mu;

   private static final String AGENT_HOSTNAME = "localhost";
   private static final String AGENT_PORT = "19266";

   @BeforeClass
   private void startPerfCakeAgent() throws InvocationTargetException, ClassNotFoundException, IllegalAccessException, InstantiationException {
      agentThread = new AgentThread("hostname=" + AGENT_HOSTNAME + ",port=" + AGENT_PORT);
      Executors.newSingleThreadExecutor().submit(agentThread);

      Properties reporterProperties = new Properties();
      reporterProperties.put("hostname", AGENT_HOSTNAME);
      reporterProperties.put("port", AGENT_PORT);
      mur = (MemoryUsageReporter) ObjectFactory.summonInstance(MemoryUsageReporter.class.getName(), reporterProperties);

      Properties destinationProperties = new Properties();
      dest = (DummyDestination) ObjectFactory.summonInstance(DummyDestination.class.getName(), destinationProperties);
   }

   @Test
   public void testMemoryUsageReporter() {
      Assert.assertNotNull(mur, "reporter's instance");
      Assert.assertEquals(mur.getHostname(), AGENT_HOSTNAME, "Agent hostname");
      Assert.assertEquals(mur.getPort(), AGENT_PORT, "Agent port");

      List<Measurement> measurementList = new LinkedList<>();

      mur.registerDestination(dest, new Period(PeriodType.ITERATION, 100));
      rm.registerReporter(mur);
      rm.setRunInfo(ri);

      rm.start();
      try {
         for (int i = 0; i < 1000; i++) {
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
      Assert.assertEquals(measurementList.size(), 11, "Number of Measurement sent to destination");
      for (Measurement m : measurementList) {
         Assert.assertNotNull(m.get("Used"), "Used memory result");
         Assert.assertNotNull(m.get("Total"), "Total memory result");
         Assert.assertNotNull(m.get("Max"), "Max memory result");
      }
   }
}
