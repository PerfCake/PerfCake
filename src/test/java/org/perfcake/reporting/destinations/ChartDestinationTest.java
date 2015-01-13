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
package org.perfcake.reporting.destinations;

import org.perfcake.PerfCakeConst;
import org.perfcake.reporting.Measurement;

import org.testng.annotations.Test;

/**
 * Chart destination tests.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ChartDestinationTest {

   @Test(enabled = true)
   public void basicTest() throws Exception {
      System.setProperty(PerfCakeConst.TIMESTAMP_PROPERTY, String.valueOf(System.currentTimeMillis()));

      ChartDestination cd = new ChartDestination();
      cd.setTarget("test-chart");
      cd.setXAxis("Time of test");
      cd.setYAxis("Iterations per second");
      cd.setName("Performance");
      cd.setGroup("stats");
      cd.setAttributes("Average, Result");

      cd.open();

      Measurement m = new Measurement(1, System.currentTimeMillis(), 1);
      m.set(10.3);
      m.set("Average", 9.8);
      cd.report(m);

      m = new Measurement(2, System.currentTimeMillis(), 2);
      m.set(11.1);
      m.set("Average", 9.1);
      cd.report(m);

      m = new Measurement(3, System.currentTimeMillis(), 3);
      m.set(9.2);
      m.set("Average", 9.0);
      cd.report(m);

      cd.close();
   }

}