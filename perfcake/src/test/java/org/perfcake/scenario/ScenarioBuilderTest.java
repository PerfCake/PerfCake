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
import org.perfcake.RunInfo;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.correlator.Correlator;
import org.perfcake.message.correlator.GenerateHeaderCorrelator;
import org.perfcake.message.generator.DefaultMessageGenerator;
import org.perfcake.message.generator.MessageGenerator;
import org.perfcake.message.receiver.HttpReceiver;
import org.perfcake.message.receiver.Receiver;
import org.perfcake.message.sender.HttpSender;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sequence.PrimitiveNumberSequence;
import org.perfcake.message.sequence.Sequence;
import org.perfcake.reporting.destination.ConsoleDestination;
import org.perfcake.reporting.destination.Destination;
import org.perfcake.reporting.reporter.IterationsPerSecondReporter;
import org.perfcake.reporting.reporter.Reporter;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.RegExpValidator;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ScenarioBuilderTest {

   @Test
   public void basicApiUsageTest() throws PerfCakeException {
      final Period period = new Period(PeriodType.TIME, 30_000);
      final RunInfo runInfo = new RunInfo(period);

      final MessageGenerator generator = new DefaultMessageGenerator();
      generator.setThreads(10);

      final MessageValidator validator = new RegExpValidator().setCaseInsensitive(true).setPattern(".*");

      final Message message = new Message();
      message.setPayload("Hello world no. @{intSeq}!");

      final MessageTemplate messageTemplate = new MessageTemplate(message, 1, Collections.singletonList("regExp"));

      final MessageSender sender = new HttpSender().setMethod(HttpSender.Method.POST).setTarget("http://httpbin.org/post");

      final Correlator correlator = new GenerateHeaderCorrelator();

      final Receiver receiver = new HttpReceiver().setSource("localhost:8282").setThreads(10);

      final Sequence sequence = new PrimitiveNumberSequence();

      final Destination destination = new ConsoleDestination();

      final Reporter reporter = new IterationsPerSecondReporter();
      reporter.registerDestination(destination, new Period(PeriodType.TIME, 1000));

      final ScenarioBuilder builder = new ScenarioBuilder(runInfo, generator, sender);
      builder.setReceiver(receiver).setCorrelator(correlator);
      builder.putMessageValidator("regExp", validator).addMessage(messageTemplate).
            putSequence("intSeq", sequence).addReporter(reporter);

      final Scenario scenario = builder.build();
      scenario.init();

      final ScenarioRetractor retractor = new ScenarioRetractor(scenario);

      Assert.assertTrue(retractor.getGenerator() instanceof DefaultMessageGenerator);
      Assert.assertEquals(retractor.getGenerator().getThreads(), 10);

      Assert.assertEquals(retractor.getMessageStore().size(), 1);
      Assert.assertEquals((long) retractor.getMessageStore().get(0).getMultiplicity(), 1);
      Assert.assertEquals(retractor.getMessageStore().get(0).getMessage().getPayload(), "Hello world no. @{intSeq}!");
      Assert.assertEquals(retractor.getMessageStore().get(0).getFilteredMessage(retractor.getSequenceManager().getSnapshot()).getPayload(), "Hello world no. 0!");
      Assert.assertEquals(retractor.getMessageStore().get(0).getValidatorIds().size(), 1);
      Assert.assertEquals(retractor.getMessageStore().get(0).getValidatorIds().get(0), "regExp");

      MessageSender tmpSender = retractor.getMessageSenderManager().acquireSender();
      Assert.assertTrue(tmpSender instanceof HttpSender);
      Assert.assertEquals(tmpSender.getTarget(), "http://httpbin.org/post");
      Assert.assertEquals(((HttpSender) tmpSender).getMethod(), HttpSender.Method.POST);
      retractor.getMessageSenderManager().releaseSender(tmpSender);

      Assert.assertEquals(retractor.getSequenceManager().getSnapshot().getProperty("intSeq"), "1");

      Assert.assertTrue(retractor.getReceiver() instanceof HttpReceiver);
      Assert.assertEquals(retractor.getReceiver().getSource(), "localhost:8282");
      Assert.assertTrue(retractor.getCorrelator() instanceof GenerateHeaderCorrelator);

      Assert.assertEquals(retractor.getReportManager().getReporters().size(), 1);
      Reporter tmpReporter = retractor.getReportManager().getReporters().iterator().next();
      Assert.assertTrue(tmpReporter instanceof IterationsPerSecondReporter);
      Assert.assertEquals(tmpReporter.getDestinations().size(), 1);
      Assert.assertEquals(tmpReporter.getReportingPeriods().size(), 1);
      BoundPeriod<Destination> boundPeriod = tmpReporter.getReportingPeriods().iterator().next();
      Assert.assertEquals(boundPeriod.getPeriodType(), PeriodType.TIME);
      Assert.assertEquals(boundPeriod.getPeriod(), 1000);
      Destination tmpDestination = tmpReporter.getDestinations().iterator().next();
      Assert.assertTrue(tmpDestination instanceof ConsoleDestination);
   }

}