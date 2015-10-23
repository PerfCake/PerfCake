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
package org.perfcake.scenario;

import org.perfcake.PerfCakeConst;
import org.perfcake.RunInfo;
import org.perfcake.TestSetup;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.PeriodType;
import org.perfcake.message.Message;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.generator.DefaultMessageGenerator;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.TestSender;
import org.perfcake.reporting.destinations.CsvDestination;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.reporting.reporters.ResponseTimeStatsReporter;
import org.perfcake.reporting.reporters.ThroughputStatsReporter;
import org.perfcake.reporting.reporters.WarmUpReporter;
import org.perfcake.util.Utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Properties;

/**
 * Verifies the correct parsing of DSL scenarios.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class DslFactoryTest extends TestSetup {

   final Properties emptyProperties = new Properties();

   @Test
   public void testDslScenarioParsing() throws Exception {
      System.setProperty(PerfCakeConst.SCENARIO_PROPERTY, "test-scenario");

      final Scenario s = ScenarioLoader.load("stub_test_scenario");
      s.init();

      Assert.assertTrue(s.getGenerator() instanceof DefaultMessageGenerator);
      Assert.assertEquals(((DefaultMessageGenerator) s.getGenerator()).getSenderTaskQueueSize(), 3000);
      Assert.assertEquals(s.getGenerator().getThreads(), 4);

      final Field runInfoField = AbstractMessageGenerator.class.getDeclaredField("runInfo");
      runInfoField.setAccessible(true);
      final RunInfo r = (RunInfo) runInfoField.get(s.getGenerator());

      Assert.assertEquals(r.getThreads(), 4);
      Assert.assertEquals(r.getDuration().getPeriodType(), PeriodType.TIME);
      Assert.assertEquals(r.getDuration().getPeriod(), 10 * 1000);

      final MessageSender ms = s.getMessageSenderManager().acquireSender();

      Assert.assertTrue(ms instanceof TestSender);
      Assert.assertEquals(((TestSender) ms).getDelay(), 12 * 1000);
      Assert.assertEquals(((TestSender) ms).getTarget(), "httpbin.org");

      final Reporter[] reporters = s.getReportManager().getReporters().toArray(new Reporter[1]);
      Assert.assertEquals(reporters.length, 3);
      Assert.assertTrue(reporters[0] instanceof WarmUpReporter);
      Assert.assertTrue(reporters[1] instanceof ThroughputStatsReporter);
      Assert.assertTrue(reporters[2] instanceof ResponseTimeStatsReporter);
      Assert.assertFalse(((ThroughputStatsReporter) reporters[1]).isMinimumEnabled());

      // only one destination should appear here as the other one is disabled
      BoundPeriod[] periods = reporters[1].getReportingPeriods().toArray(new BoundPeriod[1]);
      Assert.assertEquals(periods.length, 1);
      Assert.assertTrue(periods[0].getBinding() instanceof CsvDestination);
      Assert.assertEquals(((CsvDestination) periods[0].getBinding()).getPath(), "test-scenario-stats.csv");
      Assert.assertEquals(periods[0].getPeriodType(), PeriodType.TIME);
      Assert.assertEquals(periods[0].getPeriod(), 3 * 1000);

      periods = reporters[2].getReportingPeriods().toArray(new BoundPeriod[1]);
      Assert.assertEquals(periods.length, 1);
      Assert.assertEquals(periods[0].getPeriodType(), PeriodType.PERCENTAGE);
      Assert.assertEquals(periods[0].getPeriod(), 10);

      Assert.assertEquals(s.getMessageStore().size(), 4);
      Assert.assertEquals(s.getMessageStore().get(0).getMessage().getPayload(), Utils.readFilteredContent(Utils.locationToUrl("message1.xml", PerfCakeConst.MESSAGES_DIR_PROPERTY, "", "")));
      Assert.assertEquals((long) s.getMessageStore().get(0).getMultiplicity(), 10);
      Assert.assertEquals(s.getMessageStore().get(0).getMessage().getProperties().size(), 0);
      Assert.assertEquals(s.getMessageStore().get(0).getMessage().getHeaders().size(), 0);
      Assert.assertEquals(s.getMessageStore().get(0).getValidatorIds().size(), 0);

      Assert.assertEquals(s.getMessageStore().get(1).getMessage().getPayload(), "Hello World");
      Assert.assertEquals(s.getMessageStore().get(1).getMessage().getProperties().size(), 1);
      Assert.assertEquals(s.getMessageStore().get(1).getMessage().getProperties().get("values"), new int[] { 1, 2, 3 });
      Assert.assertEquals(s.getMessageStore().get(1).getMessage().getHeaders().size(), 0);
      Assert.assertEquals(s.getMessageStore().get(1).getValidatorIds().size(), 0);

      Assert.assertEquals(s.getMessageStore().get(2).getMessage().getPayload(), Utils.readFilteredContent(Utils.locationToUrl("message2.txt", PerfCakeConst.MESSAGES_DIR_PROPERTY, "", "")));
      Assert.assertEquals(s.getMessageStore().get(2).getMessage().getProperties().size(), 0);
      Assert.assertEquals(s.getMessageStore().get(2).getMessage().getHeaders().size(), 0);
      Assert.assertEquals(s.getMessageStore().get(2).getValidatorIds().size(), 2);
      Assert.assertEquals(s.getMessageStore().get(2).getValidatorIds().get(0), "text1");
      Assert.assertEquals(s.getMessageStore().get(2).getValidatorIds().get(1), "text2");

      Assert.assertEquals(s.getMessageStore().get(3).getMessage().getPayload(), "Simple text");
      Assert.assertEquals(s.getMessageStore().get(3).getMessage().getProperties().size(), 1);
      Assert.assertEquals(s.getMessageStore().get(3).getMessage().getProperty("propA"), "kukuk");
      Assert.assertEquals(s.getMessageStore().get(3).getMessage().getHeaders().size(), 2);
      Assert.assertEquals(s.getMessageStore().get(3).getMessage().getHeader("name"), "Franta");
      Assert.assertEquals(s.getMessageStore().get(3).getMessage().getHeaders().get("count"), 10);
      Assert.assertEquals(s.getMessageStore().get(3).getValidatorIds().size(), 2);
      Assert.assertEquals(s.getMessageStore().get(3).getValidatorIds().get(0), "text1");
      Assert.assertEquals(s.getMessageStore().get(3).getValidatorIds().get(1), "text2");

      Assert.assertFalse(s.getValidationManager().isEnabled());
      Assert.assertTrue(s.getValidationManager().isFastForward());

      final Message toValidate = new Message();
      toValidate.setPayload("I am a fish!");
      Assert.assertTrue(s.getValidationManager().getValidators(Collections.singletonList("text1")).get(0).isValid(null, toValidate, emptyProperties));

      toValidate.setPayload("I was a fish!");
      Assert.assertTrue(s.getValidationManager().getValidators(Collections.singletonList("text2")).get(0).isValid(null, toValidate, emptyProperties));

      System.clearProperty(PerfCakeConst.SCENARIO_PROPERTY);
   }
}