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
package org.perfcake.debug;

import org.perfcake.PerfCakeConst;
import org.perfcake.ScenarioExecution;
import org.perfcake.TestSetup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.Query;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "integration")
public class PerfCakeDebugTest extends TestSetup {

   private static final Logger log = LogManager.getLogger(PerfCakeDebugTest.class);

   @Test
   public void runAgent() throws Exception {
      final Properties props = new Properties();
      props.setProperty(PerfCakeConst.DEBUG_PROPERTY, "true");
      props.setProperty("perfcake.test.duration", "1000");
      props.setProperty("org.jboss.byteman.compileToBytecode", "true");
      ScenarioExecution.execute("test-debug-agent", props);

      final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      final Set<ObjectInstance> instances = server.queryMBeans(null, Query.isInstanceOf(Query.value("com.netflix.servo.jmx.MonitorMBean")));
      log.info(instances.toString());
      final Map<String, String> agentResults = new HashMap<>();
      instances.forEach(v -> {
         try {
            for (final MBeanAttributeInfo info : server.getMBeanInfo(v.getObjectName()).getAttributes()) {
               final String objectName = v.getObjectName().toString();
               final String[] parts = objectName.split("[:,]");
               final String name = (objectName.contains("INFO") ? "i:" : "c:") + parts[1] + (parts[2].startsWith("category") ? "," + parts[2] : "") + (parts[3].startsWith("category") ? "," + parts[3] : "");
               agentResults.put(name, server.getAttribute(v.getObjectName(), info.getName()).toString());
            }
         } catch (Exception e) {
            Assert.fail("Could not communicate with the MBean server.", e);
         }
      });

      Assert.assertEquals(agentResults.get("i:name=ReceiverClassName"), "org.perfcake.message.receiver.DummyReceiver");
      Assert.assertEquals(agentResults.get("i:name=Validation,category1=validator1"), "org.perfcake.validation.DummyValidator");
      Assert.assertEquals(agentResults.get("i:name=GeneratorClassName"), "org.perfcake.message.generator.DefaultMessageGenerator");
      Assert.assertEquals(agentResults.get("i:name=CorrelatorClassName"), "org.perfcake.message.correlator.DummyCorrelator");
      Assert.assertEquals(agentResults.get("i:name=SenderClassName"), "org.perfcake.message.sender.TestSender");
      Assert.assertEquals(agentResults.get("i:name=Sequences,category1=seq.number"), "org.perfcake.message.sequence.PrimitiveNumberSequence");
      Assert.assertEquals(agentResults.get("c:name=Validation,category1=validator1,category2=failed"), "0");

      Assert.assertTrue(Integer.valueOf(agentResults.get("c:name=Reporting,category1=org.perfcake.reporting.reporter.IterationsPerSecondReporter,category2=org.perfcake.reporting.destination.ConsoleDestination")) > 0);
      Assert.assertTrue(Integer.valueOf(agentResults.get("c:name=Reporting,category1=org.perfcake.reporting.reporter.IterationsPerSecondReporter")) > 0);
      Assert.assertTrue(Integer.valueOf(agentResults.get("c:name=Validation,category1=validator1,category2=passed")) > 0);
      Assert.assertTrue(Integer.valueOf(agentResults.get("c:name=SentMessages")) > 0);
      Assert.assertTrue(Integer.valueOf(agentResults.get("c:name=CorrelatedMessages")) > 0);
      Assert.assertTrue(Integer.valueOf(agentResults.get("c:name=SequenceSnapshots")) > 0);
      Assert.assertTrue(Integer.valueOf(agentResults.get("c:name=Validation,category1=validator1")) > 0);
      Assert.assertTrue(Integer.valueOf(agentResults.get("c:name=GeneratedSenderTasks")) > 0);
   }
}