package org.perfcake;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ScenarioExcutionTest {
   private Scenario scenario;
   private ExecutorService es;

   @BeforeTest
   public void prepareScenario() throws PerfCakeException, Exception {
      System.setProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY, getClass().getResource("/scenarios").getPath());
      System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, getClass().getResource("/messages").getPath());
      ScenarioBuilder builder = new ScenarioBuilder().load("test-dummy-scenario");
      scenario = builder.build();
   }

   @Test
   public void stopScenarioExecutionTest() throws PerfCakeException {
      scenario.init();
      final long pre = System.currentTimeMillis();
      es = Executors.newSingleThreadExecutor();
      es.submit(new ScenarioExecutorStopper(scenario, 5));
      es.shutdown();
      scenario.run();
      final long post = System.currentTimeMillis();
      final long diff = post - pre;
      Assert.assertTrue(diff > 5000 && diff < 6000);
   }

   @Test
   public void fullScenarioExecutionTest() throws PerfCakeException {
      scenario.init();
      final long pre = System.currentTimeMillis();
      scenario.run();
      final long post = System.currentTimeMillis();
      final long diff = post - pre;
      Assert.assertTrue(diff > 10000 && diff < 11000);
   }

   private class ScenarioExecutorStopper implements Runnable {
      private Scenario scenario;
      private int delay;

      public ScenarioExecutorStopper(Scenario scenario, int delay) {
         this.scenario = scenario;
         this.delay = delay;
      }

      @Override
      public void run() {
         for (int i = delay; i > 0; i--) {
            System.out.println("Stopping scenario execution in " + i + "s.");
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }
         System.out.println("Stopping scenario execution...");
         scenario.stop();
      }
   }
}
