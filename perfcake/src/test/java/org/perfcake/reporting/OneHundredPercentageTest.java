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
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.reporting;

import org.perfcake.RunInfo;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.DefaultMessageGenerator;
import org.perfcake.message.sender.TestSender;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.reporting.reporters.ThroughputStatsReporter;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioBuilder;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verify that 100% result is always reported no matter what periods are configured.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "integration" })
public class OneHundredPercentageTest {

   private Scenario getScenario(final Period p, final DummyDestination dd) throws Exception {
      final RunInfo ri = new RunInfo(p);

      final DefaultMessageGenerator mg = new DefaultMessageGenerator();
      mg.setThreads(100);
      mg.setSenderTaskQueueSize(1000);

      final ThroughputStatsReporter atr = new ThroughputStatsReporter();
      atr.registerDestination(dd, new Period(PeriodType.TIME, 500));

      final Message m = new Message();
      m.setPayload("hello");

      final MessageTemplate mt = new MessageTemplate(m, 1, null);

      final ScenarioBuilder sb = new ScenarioBuilder(ri, mg, TestSender.class.getName(), null);
      sb.addReporter(atr);
      sb.addMessage(mt);

      return sb.build();
   }

   @Test
   public void makeSure100PercentageTimeIsReported() throws Exception {
      final DummyDestination dd = new DummyDestination();
      final Scenario s = getScenario(new Period(PeriodType.TIME, 2000), dd);
      s.init();
      s.run();

      Assert.assertEquals(dd.getLastMeasurement().getPercentage(), 100l);
   }

   @Test
   public void makeSure100PercentageIterationIsReported() throws Exception {
      final DummyDestination dd = new DummyDestination();
      final Scenario s = getScenario(new Period(PeriodType.ITERATION, 2000), dd);
      s.init();
      s.run();

      Assert.assertEquals(dd.getLastMeasurement().getPercentage(), 100l);
   }
}
