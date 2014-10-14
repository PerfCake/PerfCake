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
import org.perfcake.common.PeriodType;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.generator.DefaultMessageGenerator;
import org.perfcake.message.sender.DummySender;
import org.perfcake.message.sender.JmsSender;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.RequestResponseJmsSender;

import org.apache.commons.beanutils.BeanUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Verifies the correct parsing of DSL scenarios.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class DSLFactoryTest {

   @Test
   public void testDslScenarioParsing() throws Exception {
      System.setProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY, getClass().getResource("/scenarios").getPath());
      System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, getClass().getResource("/messages").getPath());

      Scenario s = ScenarioLoader.load("stub_test_scenario");
      s.init();

      Assert.assertTrue(s.getGenerator() instanceof DefaultMessageGenerator);
      Assert.assertEquals(((DefaultMessageGenerator) s.getGenerator()).getThreadQueueSize(), 3000);
      Assert.assertEquals(s.getGenerator().getThreads(), 4);

      Field runInfoField = AbstractMessageGenerator.class.getDeclaredField("runInfo");
      runInfoField.setAccessible(true);
      RunInfo r = (RunInfo) runInfoField.get(s.getGenerator());

      Assert.assertEquals(r.getThreads(), 4);
      Assert.assertEquals(r.getDuration().getPeriodType(), PeriodType.TIME);
      Assert.assertEquals(r.getDuration().getPeriod(), 10 * 1000);

      MessageSender ms = s.getMessageSenderManager().acquireSender();

      Assert.assertTrue(ms instanceof DummySender);
      Assert.assertEquals(((DummySender) ms).getDelay(), 12 * 1000);
      Assert.assertEquals(((DummySender) ms).getTarget(), "httpbin.org");

   }

   @Test
   public void testDescribe() throws Exception {
      MessageSender ms = new RequestResponseJmsSender();
      ((JmsSender) ms).setPassword("abc");

      Map m = BeanUtils.describe(ms);

      System.out.println(m);
   }
}