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
package org.perfcake.common;

import org.perfcake.RunInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the {@link org.perfcake.RunInfo} conditions.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class RunInfoTest {
   private static final Logger log = LogManager.getLogger(RunInfoTest.class);

   @Test
   public void runInfoIterationBasedState() throws InterruptedException {
      final Period p = new Period(PeriodType.ITERATION, 100);
      final RunInfo ri = new RunInfo(p);

      Assert.assertEquals(ri.getDuration(), p);

      log.info("NEW       " + ri);
      Assert.assertEquals(ri.getStartTime(), -1);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), -1);
      Assert.assertFalse(ri.isStarted());
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 0d);

      ri.start();
      log.info("START   0 " + ri);
      long startTime = ri.getStartTime();
      Assert.assertTrue(startTime > 0);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), -1);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 0d);

      ri.getNextIteration();
      log.info("START   1 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 0);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 1d);

      for (int i = 0; i < 20; i++) {
         ri.getNextIteration();
      }
      log.info("START  20 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 20);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 21d);

      Thread.sleep(10); // make sure the startTime will be different after reset
      ri.reset();
      log.info("RESET     " + ri);
      Assert.assertNotEquals(ri.getStartTime(), startTime);
      startTime = ri.getStartTime();
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), -1);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 0d);

      for (int i = 0; i < 50; i++) {
         ri.getNextIteration();
      }
      log.info("START  50 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 49);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 50d);

      for (int i = 0; i < 50; i++) {
         ri.getNextIteration();
      }
      log.info("START 100 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 99);
      Assert.assertTrue(ri.isStarted());
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 100d);

      ri.getNextIteration();
      log.info("START 101 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 100);
      Assert.assertTrue(ri.isStarted());
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 100d);

      Thread.sleep(10);
      ri.stop();
      log.info("STOP      " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertTrue(ri.getEndTime() > startTime);
      Assert.assertEquals(ri.getIteration(), 100);
      Assert.assertFalse(ri.isStarted());
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 100d); // after stop, we cannot get over 100
   }

   @Test
   public void runInfoTimeBasedState() throws InterruptedException {
      final Period p = new Period(PeriodType.TIME, 3000);
      final RunInfo ri = new RunInfo(p);

      Assert.assertEquals(ri.getDuration(), p);

      log.info("NEW       " + ri);
      Assert.assertEquals(ri.getStartTime(), -1);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), -1);
      Assert.assertFalse(ri.isStarted());
      Assert.assertFalse(ri.isRunning());
      Assert.assertTrue(ri.getPercentage() < 10d);

      ri.start();
      log.info("START   0 " + ri);
      long startTime = ri.getStartTime();
      Assert.assertTrue(startTime > 0);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), -1);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertTrue(ri.getPercentage() < 50d);

      ri.getNextIteration();
      log.info("START   1 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 0);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertTrue(ri.getPercentage() < 50d);

      for (int i = 0; i < 20; i++) {
         ri.getNextIteration();
      }
      log.info("START  20 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 20);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertTrue(ri.getPercentage() < 50d);

      Thread.sleep(10); // make sure the startTime will be different after reset
      ri.reset();
      log.info("RESET     " + ri);
      Assert.assertNotEquals(ri.getStartTime(), startTime);
      startTime = ri.getStartTime();
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), -1);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertTrue(ri.getPercentage() < 10d); // we should be at the very beginning

      for (int i = 0; i < 50; i++) {
         ri.getNextIteration();
      }
      log.info("START  50 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 49);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertTrue(ri.getPercentage() < 50d); // we should not be that far

      for (int i = 0; i < 50; i++) {
         ri.getNextIteration();
      }
      log.info("START 100 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 99);
      Assert.assertTrue(ri.isStarted());
      Assert.assertTrue(ri.isRunning());
      Assert.assertTrue(ri.getPercentage() < 50d);  // we should not be that far

      Thread.sleep(3000); // wait to reach 100%

      ri.getNextIteration();
      log.info("START 101 " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertEquals(ri.getEndTime(), -1);
      Assert.assertEquals(ri.getIteration(), 100);
      Assert.assertTrue(ri.isStarted());
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 100d); // we cannot get over 100

      ri.stop();
      log.info("STOP      " + ri);
      Assert.assertEquals(ri.getStartTime(), startTime);
      Assert.assertTrue(ri.getEndTime() > startTime);
      Assert.assertEquals(ri.getIteration(), 100);
      Assert.assertFalse(ri.isStarted());
      Assert.assertFalse(ri.isRunning());
      Assert.assertEquals(ri.getPercentage(), 100d); // we cannot get over 100
   }
}
