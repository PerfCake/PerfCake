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

import org.perfcake.PerfCakeException;
import org.perfcake.RunInfo;
import org.perfcake.TestSetup;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.DefaultMessageGenerator;
import org.perfcake.message.generator.MessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.reporting.reporters.DummyReporter;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.reporting.reporters.WarmUpReporter;
import org.perfcake.util.Utils;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.RegExpValidator;
import org.perfcake.validation.ValidationManager;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Verifies the correct parsing of XML scenarios.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class XmlFactoryTest extends TestSetup {
   private static final int THREADS = 10;
   private static final String MESSAGE1_CONTENT = "Stupid is as supid does! :)";
   private static final String MESSAGE2_CONTENT = "I'm the fish!";
   private static final String SENDER_CLASS = "org.perfcake.message.sender.HttpSender";
   private static final String FILTERED_PROPERTY_VALUE = "filtered-property-value";
   private static final String DEFAULT_PROPERTY_VALUE = "default-property-value";

   final Properties emptyProperties = new Properties();

   @BeforeClass
   public void prepareScenarioParser() throws PerfCakeException, URISyntaxException, IOException {
      System.setProperty("test.filtered.property", FILTERED_PROPERTY_VALUE);
   }

   @Test
   public void parseScenarioPropertiesTest() throws Exception {
      try {
         final XmlFactory scenarioFactory = new XmlFactory();
         scenarioFactory.init(Utils.getResourceAsUrl("/scenarios/test-scenario.xml"));
         final Hashtable<Object, Object> scenarioProperties = scenarioFactory.parseScenarioProperties();
         Assert.assertEquals(scenarioProperties.get("quickstartName"), "testQS", "quickstartName property");
         Assert.assertEquals(scenarioProperties.get("filteredProperty"), FILTERED_PROPERTY_VALUE, "filteredProperty property");
         Assert.assertEquals(scenarioProperties.get("defaultProperty"), DEFAULT_PROPERTY_VALUE, "defaultProperty property");
      } catch (final PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseSenderTest() throws Exception {
      try {
         final XmlFactory scenarioFactory = new XmlFactory();
         scenarioFactory.init(Utils.getResourceAsUrl("/scenarios/test-scenario.xml"));
         final MessageSenderManager senderManager = scenarioFactory.parseSender(THREADS);
         Assert.assertEquals(senderManager.getSenderClass(), SENDER_CLASS, "senderClass");
         Assert.assertEquals(senderManager.getSenderPoolSize(), THREADS, "senderPoolSize");
         Assert.assertEquals(System.getProperty("propWithQsName", ""), "testQS-name");
         // TODO: add assertions on a sender
      } catch (final PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseGeneratorTest() throws Exception {
      try {
         final XmlFactory scenarioFactory = new XmlFactory();
         scenarioFactory.init(Utils.getResourceAsUrl("/scenarios/test-scenario.xml"));
         final MessageGenerator generator = scenarioFactory.parseGenerator();
         Assert.assertTrue(generator instanceof DefaultMessageGenerator, "The generator is not an instance of " + DefaultMessageGenerator.class.getName());
         final DefaultMessageGenerator dmg = (DefaultMessageGenerator) generator;
         dmg.setRunInfo(new RunInfo(new Period(PeriodType.TIME, 30L)));
         Assert.assertEquals(dmg.getThreads(), THREADS, "threads");
         Assert.assertEquals(dmg.getSenderTaskQueueSize(), 5000);
      } catch (final PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseMessagesTest() throws Exception {
      try {
         final XmlFactory scenarioFactory = new XmlFactory();
         scenarioFactory.init(Utils.getResourceAsUrl("/scenarios/test-scenario.xml"));
         // Message store
         ValidationManager validationManager = scenarioFactory.parseValidation();
         final List<MessageTemplate> messageStore = scenarioFactory.parseMessages(validationManager);
         Assert.assertEquals(messageStore.size(), 5);

         // Message 1
         final MessageTemplate mts1 = messageStore.get(0);
         Assert.assertEquals(mts1.getMultiplicity(), new Long(10), "message1 multiplicity");
         final Message m1 = mts1.getMessage();
         // Message 1 content
         Assert.assertEquals(m1.getPayload(), MESSAGE1_CONTENT, "message1 content");
         // Message 1 headers
         final Properties headers1 = m1.getHeaders();
         Assert.assertEquals(headers1.size(), 3, "message1 headers count");
         Assert.assertEquals(headers1.get("m_header1"), "m_h_value1", "message1 header1");
         Assert.assertEquals(headers1.get("m_header2"), "m_h_value2", "message1 header2");
         Assert.assertEquals(headers1.get("m_header3"), "m_h_value3", "message1 header3");
         // Message 1 properties
         final Properties properties1 = m1.getProperties();
         Assert.assertEquals(properties1.size(), 3, "message1 properties count");
         Assert.assertEquals(properties1.get("m_property1"), "m_p_value1", "message1 property1");
         Assert.assertEquals(properties1.get("m_property2"), "m_p_value2", "message1 property2");
         Assert.assertEquals(properties1.get("m_property3"), "m_p_value3", "message1 property3");
         // Message 1 validatorIds
         final List<MessageValidator> validatorsList1 = validationManager.getValidators(mts1.getValidatorIds());
         Assert.assertEquals(validatorsList1.size(), 2, "message1 validatorIdList size");
         Assert.assertTrue(validatorsList1.get(0).isValid(null, new Message("Hello, this is Stupid validator"), emptyProperties));
         Assert.assertFalse(validatorsList1.get(0).isValid(null, new Message("Hello, this is Smart validator"), emptyProperties));
         Assert.assertTrue(validatorsList1.get(1).isValid(null, new Message("Hello, this is happy validator :)"), emptyProperties));
         Assert.assertFalse(validatorsList1.get(1).isValid(null, new Message("Hello, this is sad validator :("), emptyProperties));

         // Message 2
         final MessageTemplate mts2 = messageStore.get(1);
         Assert.assertEquals(mts2.getMultiplicity(), new Long(1), "message2 multiplicity");
         final Message m2 = mts2.getMessage();
         // Message 2 content
         Assert.assertEquals(m2.getPayload(), MESSAGE2_CONTENT, "message2 content");
         // Message 2 headers
         final Properties headers2 = m2.getHeaders();
         Assert.assertEquals(headers2.size(), 0, "message2 headers count");
         // Message 2 properties
         final Properties properties2 = m2.getProperties();
         Assert.assertEquals(properties2.size(), 0, "message2 properties count");
         // Message 2 validatorIds
         final List<MessageValidator> validatorsList2 = validationManager.getValidators(mts2.getValidatorIds());
         Assert.assertEquals(validatorsList2.size(), 1, "message2 validatorIdList size");
         Assert.assertTrue(validatorsList2.get(0).isValid(null, new Message("Go for fishing!"), emptyProperties));
         Assert.assertFalse(validatorsList2.get(0).isValid(null, new Message("Go for mushroom picking! There are no Fish."), emptyProperties));

         // Message 3
         final MessageTemplate mts3 = messageStore.get(2);
         final Message m3 = mts3.getMessage();
         Assert.assertNotNull(m3, "message 3 instance");
         Assert.assertNull(m3.getPayload(), "message 3 payload");
         Assert.assertEquals(m3.getHeaders().size(), 1, "message 3 header count");
         Assert.assertEquals(m3.getHeader("h3_name"), "h3_value", "message 3 header value");

         // Message 4
         final MessageTemplate mts4 = messageStore.get(3);
         final Message m4 = mts4.getMessage();
         Assert.assertNotNull(m4, "message 4 instance");
         Assert.assertEquals(m4.getPayload(), "message-content-4");

         // Message 5
         final MessageTemplate mts5 = messageStore.get(4);
         final Message m5 = mts5.getMessage();
         Assert.assertNotNull(m5, "message 5 instance");
         Assert.assertEquals(m5.getPayload(), "message-content-5");

         // Messages section is optional
         final XmlFactory noMessagesScenarioFactory = new XmlFactory();
         noMessagesScenarioFactory.init(Utils.getResourceAsUrl("/scenarios/test-scenario-no-messages.xml"));
         validationManager = noMessagesScenarioFactory.parseValidation();
         final List<MessageTemplate> emptyMessageStore = noMessagesScenarioFactory.parseMessages(validationManager);
         Assert.assertTrue(emptyMessageStore.isEmpty(), "empty message store with no messages in scenario");

      } catch (final PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseReportingTest() throws Exception {
      try {
         final XmlFactory scenarioFactory = new XmlFactory();
         scenarioFactory.init(Utils.getResourceAsUrl("/scenarios/test-scenario.xml"));
         final ReportManager reportManager = scenarioFactory.parseReporting();
         Assert.assertNotNull(reportManager);
         Assert.assertEquals(reportManager.getReporters().size(), 2, "reportManager's number of reporters");
         final String DUMMY_REPORTER_KEY = "dummy";
         final String WARM_UP_REPORTER_KEY = "warmup";

         final Map<String, Reporter> reportersMap = new HashMap<>();
         for (final Reporter reporter : reportManager.getReporters()) {
            if (reporter instanceof DummyReporter) {
               reportersMap.put(DUMMY_REPORTER_KEY, reporter);
            } else if (reporter instanceof WarmUpReporter) {
               reportersMap.put(WARM_UP_REPORTER_KEY, reporter);
            } else {
               Assert.fail("The reporter should be an instance of either " + DummyReporter.class.getCanonicalName() + " or " + WarmUpReporter.class.getCanonicalName() + ", but it is an instance of " + reporter.getClass().getCanonicalName() + "!");
            }
         }

         final Reporter reporter = reportersMap.get(DUMMY_REPORTER_KEY);
         Assert.assertEquals(reporter.getDestinations().size(), 1, "reporter's number of destinations");
         final Destination destination = reporter.getDestinations().iterator().next();
         Assert.assertTrue(destination instanceof DummyDestination, "destination's class");
         Assert.assertEquals(((DummyDestination) destination).getProperty(), "dummy_p_value", "destination's property value");
         Assert.assertEquals(((DummyDestination) destination).getProperty2(), "dummy_p2_value", "destination's property2 value");
         int assertedPeriodCount = 0;
         Assert.assertEquals(reporter.getReportingPeriods().size(), 3);
         for (final BoundPeriod<Destination> period : reporter.getReportingPeriods()) {
            switch (period.getPeriodType()) {
               case TIME:
                  Assert.assertEquals(period.getPeriod(), 2000, "time period's value");
                  assertedPeriodCount++;
                  break;
               case ITERATION:
                  Assert.assertEquals(period.getPeriod(), 100, "iteration period's value");
                  assertedPeriodCount++;
                  break;
               case PERCENTAGE:
                  Assert.assertEquals(period.getPeriod(), 50, "percentage period's value");
                  assertedPeriodCount++;
                  break;
            }
         }
         Assert.assertEquals(assertedPeriodCount, 3, "number of period asserted");

         final Reporter warmUpReporter = reportersMap.get(WARM_UP_REPORTER_KEY);
         Assert.assertTrue(warmUpReporter instanceof WarmUpReporter, "reporter's class");
         Assert.assertEquals(warmUpReporter.getDestinations().size(), 0, "reporter's number of destinations");
         Assert.assertEquals(((WarmUpReporter) warmUpReporter).getMinimalWarmUpCount(), 12345, "reporter's minimal warmup count");
         Assert.assertEquals(((WarmUpReporter) warmUpReporter).getMinimalWarmUpDuration(), 15000, "reporter's minimal warmup duration");
         Assert.assertEquals(((WarmUpReporter) warmUpReporter).getAbsoluteThreshold(), 0.2d, "reporter's absolute threshold");
         Assert.assertEquals(((WarmUpReporter) warmUpReporter).getRelativeThreshold(), 1d, "reporter's relative threshold");

      } catch (final PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void parseValidationTest() throws Exception {
      final XmlFactory validationScenarioFactory = new XmlFactory();
      validationScenarioFactory.init(Utils.getResourceAsUrl("/scenarios/test-validator-load.xml"));
      final ValidationManager vm = validationScenarioFactory.parseValidation();
      final List<MessageTemplate> mts = validationScenarioFactory.parseMessages(vm);

      Assert.assertEquals(mts.size(), 1);
      Assert.assertEquals(mts.get(0).getValidatorIds().size(), 2);
      Assert.assertTrue(mts.get(0).getValidatorIds().contains("text1"));
      Assert.assertTrue(mts.get(0).getValidatorIds().contains("text2"));

      Assert.assertEquals(((RegExpValidator) vm.getValidator("text1")).getPattern(), MESSAGE2_CONTENT);
      Assert.assertEquals(((RegExpValidator) vm.getValidator("text2")).getPattern(), MESSAGE2_CONTENT);

      // TODO: add assertions on validation
   }

   @Test
   public void parseValidationNoValidation() throws Exception {
      final XmlFactory noValidationScenarioFactory = new XmlFactory();
      noValidationScenarioFactory.init(Utils.getResourceAsUrl("/scenarios/test-scenario-no-validation.xml"));
      final ValidationManager vm = noValidationScenarioFactory.parseValidation();
      Assert.assertEquals(vm.messagesToBeValidated(), 0);
   }

   @Test
   public void parseValidationFastForwardTest() throws Exception {
      final XmlFactory validationScenarioFactory = new XmlFactory();
      validationScenarioFactory.init(Utils.getResourceAsUrl("/scenarios/test-enable-fast-forward.xml"));

      final ValidationManager vm = validationScenarioFactory.parseValidation();

      Assert.assertTrue(vm.isFastForward(), "Fast forward did not load properly.");
   }
}
