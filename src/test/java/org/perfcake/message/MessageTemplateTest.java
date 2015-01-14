package org.perfcake.message;

import org.perfcake.PerfCakeException;
import org.perfcake.ScenarioExecution;
import org.perfcake.TestSetup;
import org.perfcake.message.sender.DummySender;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;

/**
 * @author Pavel Macík <pavel.macik@gmail.com
 */
public class MessageTemplateTest extends TestSetup {
   private static final String HELLO_NAME = "hello";
   private static final String HELLO_VALUE = "hello.value";
   private static final String NUMBER_NAME = "number";
   private static final String NUMBER_VALUE = "1";

   private static final String EXPECTED_MESSAGE_FROM_URI = NUMBER_VALUE + " 2 " + HELLO_VALUE + " 1 " + (Integer.valueOf(NUMBER_VALUE) - 1) + " " + System.getenv("JAVA_HOME") + " " + System.getProperty("java.runtime.name") + "I'm a fish!";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_1 = (Integer.valueOf(NUMBER_VALUE) + 1) + "@{missing1}";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_2 = "@{missing2}";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_3 = "${missing3}";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_4 = "@{missing4a}${missing4b}";
   private static final String EXPECTED_MESSAGE_FROM_CONTENT_5 = "${missing5a}@{missing5b}";


   private ScenarioExecution execution;
   private Scenario scenario;
   private DummySender dummySender;

   @Test
   public void messageTemplateFilteringTest() throws PerfCakeException {
      ScenarioLoader sl = new ScenarioLoader();
      Scenario scenario = sl.load("test-scenario-unfiltered");
      ScenarioRetractor sr = new ScenarioRetractor(scenario);
      List<MessageTemplate> messageStore = sr.getMessageStore();
      Assert.assertEquals(messageStore.size(), 6);

      Properties propertiesToBeFiltered = new Properties();
      propertiesToBeFiltered.put(HELLO_NAME, HELLO_VALUE);
      propertiesToBeFiltered.put(NUMBER_NAME, NUMBER_VALUE) ;
      Message m0 = messageStore.get(0).getFilteredMessage(propertiesToBeFiltered);
      System.out.println(m0.getPayload());
      Assert.assertEquals(m0.getPayload(), EXPECTED_MESSAGE_FROM_URI);

      Message m1 = messageStore.get(1).getFilteredMessage(propertiesToBeFiltered);
      System.out.println("M1:" + m1.getPayload());
      Assert.assertEquals(m1.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_1);

      Message m2 = messageStore.get(2).getFilteredMessage(propertiesToBeFiltered);
      System.out.println("M2:" + m2.getPayload());
      //Assert.assertEquals(m2.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_2);

      Message m3 = messageStore.get(3).getFilteredMessage(propertiesToBeFiltered);
      System.out.println("M3:" + m3.getPayload());
      //Assert.assertEquals(m3.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_3);

      Message m4 = messageStore.get(4).getFilteredMessage(propertiesToBeFiltered);
      System.out.println("M4:" + m4.getPayload());
      //Assert.assertEquals(m4.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_4);

      Message m5 = messageStore.get(5).getFilteredMessage(propertiesToBeFiltered);
      System.out.println("M5:" + m3.getPayload());
      //Assert.assertEquals(m5.getPayload(), EXPECTED_MESSAGE_FROM_CONTENT_5);
   }
}
