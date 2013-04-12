/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.reporting;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA. User: fnguyen Date: 8/9/11 Time: 1:05 PM To change
 * this template use File | Settings | File Templates.
 */
public class PercentageTest extends ReportingTestBase {

   private static float ERR_MARGIN = 0.001f;

   @Test
   public void durationSet() {

      TestRunInfo tri = new TestRunInfo();
      tri.setTestDuration(10);// 10 seconds
      tri.setTestStartTime(System.currentTimeMillis());
      Assert.assertTrue(Math.abs(tri.getPercentage()) < ERR_MARGIN);
      sleep(1);
      Assert.assertTrue(Math.abs(tri.getPercentage() - 0.1f) < ERR_MARGIN);

      sleep(1.5f);
      Assert.assertTrue(Math.abs(tri.getPercentage() - 0.25f) < ERR_MARGIN);

      tri = new TestRunInfo();
      tri.setTestDuration(1);// 10 seconds
      tri.setTestStartTime(System.currentTimeMillis());
      sleep(0.5f);
      Assert.assertTrue(Math.abs(tri.getPercentage() - 0.5f) < ERR_MARGIN);
      sleep(0.6f);
      Assert.assertTrue(Math.abs(tri.getPercentage() - 1f) < ERR_MARGIN);

   }

   @Test
   public void iterationsSet() {
      TestRunInfo tri = new TestRunInfo();
      tri.setTestIterations(5);

      Assert.assertTrue(Math.abs(tri.getPercentage()) < ERR_MARGIN, String.valueOf(tri.getPercentage()));
      tri.incrementIteration();

      Assert.assertTrue(Math.abs(tri.getPercentage() - 0.20f) < ERR_MARGIN);
      tri.incrementIteration();
      tri.incrementIteration();
      tri.incrementIteration();
      tri.incrementIteration();
      tri.incrementIteration();
      Assert.assertTrue(Math.abs(tri.getPercentage() - 1f) < ERR_MARGIN);
   }
}
