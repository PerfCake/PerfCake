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
package org.perfcake.message;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.TestUtil;
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

   @Test
   public void messageTemplateFilteringTest() throws PerfCakeException {
      final ScenarioLoader sl = new ScenarioLoader();
      final Scenario scenario = sl.load("test-scenario-unfiltered");
      final ScenarioRetractor sr = new ScenarioRetractor(scenario);
      final List<MessageTemplate> messageStore = sr.getMessageStore();
      final SequenceManager sequenceManager = sr.getSequenceManager();
      Assert.assertEquals(messageStore.size(), 7);

      Assert.assertEquals(System.getProperty("defaultProperty"), "default-property-value");
      Assert.assertEquals(System.getProperty("composedProperty"), "default-property-value2");

      final Properties propertiesToBeFiltered = new Properties();
      sequenceManager.getSnapshot().forEach(propertiesToBeFiltered::put);
      propertiesToBeFiltered.setProperty(HELLO_NAME, HELLO_VALUE);
      propertiesToBeFiltered.setProperty(NUMBER_NAME, "1");
      final Message m0 = messageStore.get(0).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m0.getPayload(), "1 hello.value 1 " + System.getenv("JAVA_HOME") + " " + System.getProperty("java.runtime.name") + " default-property-value2 I'm a fish!");
      Assert.assertEquals(m0.getHeader(TEST_HEADER), "1");
      Assert.assertEquals(m0.getProperty(TEST_PROPERTY), "0");

      Message m1 = messageStore.get(1).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m1.getPayload(), "1null");

      propertiesToBeFiltered.setProperty(NUMBER_NAME, "2");
      m1 = messageStore.get(1).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m1.getPayload(), "2null");

      final Message m2 = messageStore.get(2).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m2.getPayload(), "null");

      final Message m3 = messageStore.get(3).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m3.getPayload(), "null");

      final Message m4 = messageStore.get(4).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m4.getPayload(), "nullnull");

      final Message m5 = messageStore.get(5).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m5.getPayload(), "nullnull");

      final Message m6 = messageStore.get(6).getFilteredMessage(propertiesToBeFiltered);
      Assert.assertEquals(m6.getPayload(), "default-property-value2");
   }

   @Test
   public void staticTemplateTest() {
      final String runtimeName = System.getProperty("java.runtime.name");
      final Message m = new Message();
      m.setPayload("${props.java.runtime.name}${aa}");
      m.setHeader("runtime", "${props.java.runtime.name}${aa}");
      m.setProperty("name", "${props.java.runtime.name}${aa}");

      final MessageTemplate t = new MessageTemplate(m, 1, null);
      final Properties p = TestUtil.props("aa", "1");
      final Message m2 = t.getFilteredMessage(p);

      Assert.assertEquals(m2.getPayload(), runtimeName + "null");
      Assert.assertEquals(m2.getHeader("runtime"), runtimeName + "null");
      Assert.assertEquals(m2.getProperty("name"), runtimeName + "null");
      Assert.assertTrue(m2 != t.getFilteredMessage(p)); // we always get a different instance to be able to set headers properly
   }

   @Test
   public void dynamicPayloadTest() {
      final String runtimeName = System.getProperty("java.runtime.name");
      final Message m = new Message();
      m.setPayload("${props.java.runtime.name}@{aa}");
      m.setHeader("runtime", "${props.java.runtime.name}${aa}");
      m.setProperty("name", "${props.java.runtime.name}${aa}");

      final MessageTemplate t = new MessageTemplate(m, 1, null);
      final Properties p = TestUtil.props("aa", "1");
      final Message m2 = t.getFilteredMessage(p);

      Assert.assertEquals(m2.getPayload(), runtimeName + "1");
      Assert.assertEquals(m2.getHeader("runtime"), runtimeName + "null");
      Assert.assertEquals(m2.getProperty("name"), runtimeName + "null");
      Assert.assertTrue(m2 != t.getFilteredMessage(p)); // we should be getting another instance
   }

   @Test
   public void dynamicHeaderTest() {
      final String runtimeName = System.getProperty("java.runtime.name");
      final Message m = new Message();
      m.setPayload("${props.java.runtime.name}${aa}");
      m.setHeader("runtime", "${props.java.runtime.name}@{aa}");
      m.setProperty("name", "${props.java.runtime.name}${aa}");

      final MessageTemplate t = new MessageTemplate(m, 1, null);
      final Properties p = TestUtil.props("aa", "1");
      final Message m2 = t.getFilteredMessage(p);

      Assert.assertEquals(m2.getPayload(), runtimeName + "null");
      Assert.assertEquals(m2.getHeader("runtime"), runtimeName + "1");
      Assert.assertEquals(m2.getProperty("name"), runtimeName + "null");
      Assert.assertTrue(m2 != t.getFilteredMessage(p)); // we should be getting another instance
   }

   @Test
   public void dynamicPropertyTest() {
      final String runtimeName = System.getProperty("java.runtime.name");
      final Message m = new Message();
      m.setPayload("${props.java.runtime.name}${aa}");
      m.setHeader("runtime", "${props.java.runtime.name}${aa}");
      m.setProperty("name", "${props.java.runtime.name}@{aa}");

      final MessageTemplate t = new MessageTemplate(m, 1, null);
      final Properties p = TestUtil.props("aa", "1");
      final Message m2 = t.getFilteredMessage(p);

      Assert.assertEquals(m2.getPayload(), runtimeName + "null");
      Assert.assertEquals(m2.getHeader("runtime"), runtimeName + "null");
      Assert.assertEquals(m2.getProperty("name"), runtimeName + "1");
      Assert.assertTrue(m2 != t.getFilteredMessage(p)); // we should be getting another instance
   }
}
