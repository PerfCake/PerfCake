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
package org.perfcake.message;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.message.sequence.SequenceManager;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;

/**
 * Tests integration of message templates.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Test(groups = { "integration" })
public class MessageTemplateTest extends TestSetup {
   private static final String HELLO_NAME = "hello";
   private static final String HELLO_VALUE = "hello.value";
   private static final String NUMBER_NAME = "number";
   private static final String TEST_HEADER = "testHeader";
   private static final String TEST_PROPERTY = "testProperty";
   private static final int NUMBER_VALUE = 1;

   private static final String EXPECTED_MESSAGE_FROM_URI = NUMBER_VALUE + " 2 " + HELLO_VALUE + " 1 " + (NUMBER_VALUE - 1) + " " + System.getenv("JAVA_HOME") + " " + System.getProperty("java.runtime.name") + "I'm a fish!";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_1 = (NUMBER_VALUE + 1) + "null";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_1B = (NUMBER_VALUE + 2) + "null";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_2 = "null";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_3 = "null";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_4 = "nullnull";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_5 = "nullnull";

   @Test
   public void messageTemplateFilteringTest() throws PerfCakeException {
      final ScenarioLoader sl = new ScenarioLoader();
      final Scenario scenario = sl.load("test-scenario-unfiltered");
      final ScenarioRetractor sr = new ScenarioRetractor(scenario);
      final List<MessageTemplate> messageStore = sr.getMessageStore();
      final SequenceManager sequenceManager = new SequenceManager();
      Assert.assertEquals(messageStore.size(), 6);

      final Properties propertiesToBeFiltered = new Properties();
      sequenceManager.getSnapshot().forEach(propertiesToBeFiltered::put);
      propertiesToBeFiltered.setProperty(HELLO_NAME, HELLO_VALUE);
      propertiesToBeFiltered.setProperty(NUMBER_NAME, String.valueOf(NUMBER_VALUE));
      final Message m0 = messageStore.get(0).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m0.getPayload(), EXPECTED_MESSAGE_FROM_URI);
      Assert.assertEquals(m0.getHeader(TEST_HEADER), "1");
      Assert.assertEquals(m0.getProperty(TEST_PROPERTY), "0");

      Message m1 = messageStore.get(1).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m1.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_1);

      propertiesToBeFiltered.setProperty(NUMBER_NAME, String.valueOf(NUMBER_VALUE + 1));
      m1 = messageStore.get(1).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m1.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_1B);

      final Message m2 = messageStore.get(2).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m2.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_2);

      final Message m3 = messageStore.get(3).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m3.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_3);

      final Message m4 = messageStore.get(4).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m4.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_4);

      final Message m5 = messageStore.get(5).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m5.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_5);
   }
}
