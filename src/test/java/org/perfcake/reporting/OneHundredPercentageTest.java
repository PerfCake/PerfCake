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
import org.perfcake.Scenario;
import org.perfcake.ScenarioBuilder;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.DefaultMessageGenerator;
import org.perfcake.message.sender.DummySender;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.reporting.reporters.AverageThroughputReporter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verify that 100% result is always reported no matter what periods are configured.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class OneHundredPercentageTest {

   private Scenario getScenario(Period p, DummyDestination dd) throws Exception {
      ScenarioBuilder sb = new ScenarioBuilder();

      RunInfo ri = new RunInfo(p);

      sb.setRunInfo(ri);

      DefaultMessageGenerator mg = new DefaultMessageGenerator();
      mg.setThreads(100);
      mg.setThreadQueueSize(1000);

      sb.setGenerator(mg);

      DummySender ds = new DummySender();

      sb.setSender(new DummySender());

      AverageThroughputReporter atr = new AverageThroughputReporter();
      atr.setRunInfo(ri);

      atr.registerDestination(dd, new Period(PeriodType.TIME, 500));

      sb.addReporter(atr);

      Message m = new Message();
      m.setPayload("hello");

      MessageTemplate mt = new MessageTemplate(m, 1, null);

      sb.addMessage(mt);

      return sb.build();
   }

   @Test
   public void makeSure100PercentageTimeIsReported() throws Exception {
      DummyDestination dd = new DummyDestination();
      Scenario s = getScenario(new Period(PeriodType.TIME, 2000), dd);
      s.init();
      s.run();

      Assert.assertEquals(dd.getLastMeasurement().getPercentage(), 100l);
   }

   @Test
   public void makeSure100PercentageIterationIsReported() throws Exception {
      DummyDestination dd = new DummyDestination();
      Scenario s = getScenario(new Period(PeriodType.ITERATION, 2000), dd);
      s.init();
      s.run();

      Assert.assertEquals(dd.getLastMeasurement().getPercentage(), 100l);
   }
}
