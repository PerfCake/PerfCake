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
import org.perfcake.TestSetup;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Chart destination tests.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ChartDestinationTest {

   private static final Logger log = LogManager.getLogger(ChartDestinationTest.class);

   @Test(enabled = false)
   public void basicTest() throws Exception {
      System.setProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY, (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()));
      final String tempDir = TestSetup.createTempDir("test-chart");
      log.info("Created temp directory for chart: " + tempDir);

      ChartDestination cd = new ChartDestination();
      ChartDestination cd2 = new ChartDestination();
      cd.setOutputDir(tempDir);
      cd2.setOutputDir(tempDir);
      cd.setXAxis("Time of test");
      cd2.setXAxis("Time of test");
      cd.setYAxis("Iterations per second");
      cd2.setYAxis("Iterations per second");
      cd2.setxAxisType(PeriodType.ITERATION);
      cd.setName("Statistics " + (new SimpleDateFormat("HHmmss")).format(new Date()));
      cd2.setName("Performance " + (new SimpleDateFormat("HHmmss")).format(new Date()));
      cd.setGroup("stats");
      cd2.setGroup("perf");
      cd.setAttributes("Average, Result");
      cd2.setAttributes("Average, Result");

      cd.open();
      cd2.open();

      final long base = System.currentTimeMillis();
      final Random rnd = new Random();

      Measurement m = new Measurement(10, System.currentTimeMillis() - base, 1);
      m.set(10.3 + rnd.nextDouble());
      m.set("Average", 9.8 + rnd.nextDouble());
      cd.report(m);
      cd2.report(m);

      m = new Measurement(56, System.currentTimeMillis() - base, 2);
      m.set(11.1 + rnd.nextDouble());
      m.set("Average", 9.1 + rnd.nextDouble());
      cd.report(m);
      cd2.report(m);

      m = new Measurement(100, System.currentTimeMillis() - base, 3);
      m.set(9.2 + rnd.nextDouble());
      m.set("Average", 9.0 + rnd.nextDouble());
      cd.report(m);
      cd2.report(m);

      cd.close();
      cd2.close();
   }

}