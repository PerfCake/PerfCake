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

import org.perfcake.reporting.destinations.CsvDestination;
import org.perfcake.reporting.reporters.ATReporter;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.reporting.reporters.ResponseTimeReporter;
import org.testng.annotations.Test;

/**
 * This test class tests heavy load of messages and how well the reporters
 * report their's results under such heavy load.
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class StressTest extends ReportingTestBase {

   @Test
   public void atReporter() {
      Reporter reporter1 = new ATReporter();
      CsvDestination csvDestination = new CsvDestination();
      csvDestination.setProperty("outputPath", TEST_OUTPUT_DIR);
      csvDestination.setProperty("periodicity", "1.000s");

      reporter1.addDestination(csvDestination);
      reporter1.setProperty("time_window_size", "2");
      reporter1.setProperty("decimal_format", "0.0");

      ReportManager rm = new ReportManager();
      rm.setProperty("tags", "http, gateway");
      rm.setProperty("uniqueId", "stress");
      rm.loadConfigValues();
      rm.addReporter(reporter1);
      rm.assertUntouchedProperties();

      stressSimulate(rm, 10, 10000);
      // TODO
   }

   @Test
   public void atResponse() {
      Reporter reporter1 = new ResponseTimeReporter();
      CsvDestination csvDestination = new CsvDestination();
      csvDestination.setProperty("outputPath", TEST_OUTPUT_DIR);
      csvDestination.setProperty("periodicity", "1.000s");

      reporter1.addDestination(csvDestination);
      reporter1.setProperty("decimal_format", "0.000");

      ReportManager rm = new ReportManager();
      rm.setProperty("tags", "http, gateway");
      rm.setProperty("uniqueId", "stress");
      rm.loadConfigValues();
      rm.addReporter(reporter1);
      rm.assertUntouchedProperties();

      stressSimulate(rm, 10, 10000);
      // TODO

   }
}
