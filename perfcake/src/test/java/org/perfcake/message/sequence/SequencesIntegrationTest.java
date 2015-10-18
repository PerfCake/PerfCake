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
package org.perfcake.message.sequence;

import org.perfcake.TestSetup;
import org.perfcake.message.sender.TestSender;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "integration" })
public class SequencesIntegrationTest extends TestSetup {

   public void basicIntegrationTest() throws Exception {
      final Scenario scenario = ScenarioLoader.load("test-sequences");
      final List<String> targets = new ArrayList<>();

      TestSender.setOnInitListener(targets::add);
      TestSender.resetRecordings();

      scenario.init();
      scenario.run();
      Thread.sleep(500);
      scenario.close();

      TestSender.setOnInitListener(null);

      final List<String> messages = TestSender.getRecordedMessages();

      Assert.assertEquals(messages.size(), 10);

      // First iteration, sequences has the same values as these differe only for different iterations
      // Dollar signed properties are only qsName, @-signed properties got properly replaced
      Assert.assertEquals(messages.get(0), "counter:null-counter:0,double:null-double:0-testQS,testQS");
      Assert.assertEquals(messages.get(1), "Test message: counter:null-counter:0,double:null-double:0-testQS,testQS");

      Assert.assertEquals(messages.get(2), "counter:null-counter:1,double:null-double:2-testQS,testQS");
      Assert.assertEquals(messages.get(3), "Test message: counter:null-counter:1,double:null-double:2-testQS,testQS");

      Assert.assertEquals(messages.get(4), "counter:null-counter:2,double:null-double:4-testQS,testQS");
      Assert.assertEquals(messages.get(5), "Test message: counter:null-counter:2,double:null-double:4-testQS,testQS");

      Assert.assertEquals(messages.get(6), "counter:null-counter:3,double:null-double:6-testQS,testQS");
      Assert.assertEquals(messages.get(7), "Test message: counter:null-counter:3,double:null-double:6-testQS,testQS");

      Assert.assertEquals(targets.size(), 10);
      Assert.assertEquals(targets.get(0), "test-null-0");
      Assert.assertEquals(targets.get(1), "test-null-0");
      Assert.assertEquals(targets.get(2), "test-null-1");
      Assert.assertEquals(targets.get(3), "test-null-1");
      Assert.assertEquals(targets.get(4), "test-null-2");
      Assert.assertEquals(targets.get(5), "test-null-2");
      Assert.assertEquals(targets.get(6), "test-null-3");
      Assert.assertEquals(targets.get(7), "test-null-3");
      Assert.assertEquals(targets.get(8), "test-null-4");
      Assert.assertEquals(targets.get(9), "test-null-4");

      ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      Assert.assertEquals(retractor.getSequenceManager().getSnapshot().getProperty("counterPlus"), "15");
   }
}