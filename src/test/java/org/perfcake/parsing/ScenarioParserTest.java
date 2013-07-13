package org.perfcake.parsing;
 
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
 
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.message.MessageToSend;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.generator.LongtermMessageGenerator;
import org.perfcake.message.sender.AbstractSender;
import org.perfcake.message.sender.HTTPSender;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.parser.ScenarioParser;
import org.perfcake.reporting.ReportManager;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
 
public class ScenarioParserTest {
   private ScenarioParser scenarioParser;
   private static final int THREADS = 10;
   private static final int MIN_WARMUP_COUNT = 12345;
   private static final String MESSAGE1_CONTENT = "Stupid is as supid does! :)";
   private static final String MESSAGE2_CONTENT = "I'm the fish!";
   private static final String SENDER_CLASS = "org.perfcake.message.sender.HTTPSender";
 
   @BeforeClass
   public void prepareScenarioParser() throws PerfCakeException, URISyntaxException, IOException {
      scenarioParser = new ScenarioParser(getClass().getResource("/scenarios/test-scenario.xml"));
      System.getProperties().setProperty("perfcake.messages.dir", getClass().getResource("/messages").getPath());
   }
 
   @Test
   public void parseSenderTest() {
      try {
         MessageSenderManager senderManager = scenarioParser.parseSender(THREADS);
         Assert.assertEquals(senderManager.getSenderClass(), SENDER_CLASS, "senderClass");
         Assert.assertEquals(senderManager.getSenderPoolSize(), THREADS, "senderPoolSize");
         // TODO: add assertions on a sender
      } catch (PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }
 
   @Test
   public void parseGeneratorTest() {
      try {
         AbstractMessageGenerator generator = scenarioParser.parseGenerator();
         Assert.assertTrue(generator instanceof LongtermMessageGenerator, "The generator is not an instance of " + LongtermMessageGenerator.class.getName());
         LongtermMessageGenerator lmg = (LongtermMessageGenerator) generator;
         Assert.assertEquals(lmg.getMonitoringPeriod(), 1000, "monitoringPeriod"); // default value
         Assert.assertEquals(lmg.getThreadQueueSize(), 5000, "threadQueueSize");
         Assert.assertEquals(lmg.getDuration(), 30, "duration");
         Assert.assertEquals(lmg.isWarmUpEnabled(), true, "warmUpEnabled");
         Assert.assertEquals(lmg.getThreads(), THREADS, "threads");
         Assert.assertEquals(lmg.getMinimalWarmUpCount(), MIN_WARMUP_COUNT, "minimalWarmUpCount");
         Assert.assertEquals(lmg.getMinimalWarmUpDuration(), 15000, "minimalWarmUpDuration"); // default value
      } catch (PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }
 
   @Test
   public void parseMessagesTest() {
      try {
         // Message store
         List<MessageToSend> messageStore = scenarioParser.parseMessages();
         Assert.assertEquals(messageStore.size(), 2);
 
         // Message 1
         MessageToSend mts1 = messageStore.get(0);
         Assert.assertEquals(mts1.getMultiplicity(), new Long(10), "message1 multiplicity");
         Message m1 = mts1.getMessage();
         // Message 1 content
         Assert.assertEquals(m1.getPayload(), MESSAGE1_CONTENT, "message1 content");
         // Message 1 headers
         Properties headers1 = m1.getHeaders();
         Assert.assertEquals(headers1.size(), 3, "message1 headers count");
         Assert.assertEquals(headers1.get("m_header1"), "m_h_value1", "message1 header1");
         Assert.assertEquals(headers1.get("m_header2"), "m_h_value2", "message1 header2");
         Assert.assertEquals(headers1.get("m_header3"), "m_h_value3", "message1 header3");
         // Message 1 properties
         Properties properties1 = m1.getProperties();
         Assert.assertEquals(properties1.size(), 3, "message1 properties count");
         Assert.assertEquals(properties1.get("m_property1"), "m_p_value1", "message1 property1");
         Assert.assertEquals(properties1.get("m_property2"), "m_p_value2", "message1 property2");
         Assert.assertEquals(properties1.get("m_property3"), "m_p_value3", "message1 property3");
 
         // Message 2
         MessageToSend mts2 = messageStore.get(1);
         Assert.assertEquals(mts2.getMultiplicity(), new Long(1), "message2 multiplicity");
         Message m2 = mts2.getMessage();
         // Message 2 content
         Assert.assertEquals(m2.getPayload(), MESSAGE2_CONTENT, "message2 content");
         // Message 2 headers
         Properties headers2 = m2.getHeaders();
         Assert.assertEquals(headers2.size(), 0, "message2 headers count");
         // Message 2 properties
         Properties properties2 = m2.getProperties();
         Assert.assertEquals(properties2.size(), 0, "message2 properties count");
 
      } catch (PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }
 
   @Test(enabled = false)
   public void parseReportingTest() {
      try {
         ReportManager reportManager = scenarioParser.parseReporting();
         // TODO: add assertions on reportManager
      } catch (PerfCakeException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }
 
   @Test(enabled = false)
   public void scenarioPropertiesTest() {
      // TODO: add scenario properties test ones it is implemented
   }
 
}