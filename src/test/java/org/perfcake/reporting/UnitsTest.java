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
import org.perfcake.reporting.destinations.util.CsvFile;
import org.perfcake.reporting.reporters.ATReporter;
import org.perfcake.reporting.reporters.LabelTypes;
import org.perfcake.reporting.reporters.Reporter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * This unit tests demonstrates capability of reporting to use "units" thus
 * reporting after specific unit passed (second, iteration, percentage).
 */
public class UnitsTest extends ReportingTestBase {

   /**
    * Percentage periodicity can be set only to tests that have predefined
    * number of iterations or predefined time (which is always the case).
    */
   @Test
   public void percents() {
      Reporter reporter1 = new ATReporter();
      CsvDestination csvDestination = new CsvDestination();
      csvDestination.setProperty("outputPath", TEST_OUTPUT_DIR);
      csvDestination.setProperty("periodicity", "10%");

      reporter1.addDestination(csvDestination);
      reporter1.setProperty("time_window_size", "2");
      reporter1.setProperty("decimal_format", "0.0");

      ReportManager rm = new ReportManager();
      rm.getTestRunInfo().setTestIterations(10);
      rm.setProperty("tags", "http, gateway");
      rm.setProperty("uniqueId", "test");
      rm.loadConfigValues();
      rm.addReporter(reporter1);
      rm.assertUntouchedProperties();
      simulateIterations(rm);

      CsvFile csvFile = new CsvFile(TEST_OUTPUT_DIR + "/test_" + MeasurementTypes.AI_TOTAL + "_" + LabelTypes.ITERATIONS + ".csv");
      Assert.assertEquals(11, csvFile.getLines().size());
   }

   /**
    * Iterations periodicity just appends into destinations after specified
    * amount of iterations.
    */
   @Test
   public void iterations() {
      Reporter reporter1 = new ATReporter();
      CsvDestination csvDestination = new CsvDestination();
      csvDestination.setProperty("outputPath", TEST_OUTPUT_DIR);
      csvDestination.setProperty("periodicity", "1 it");

      reporter1.addDestination(csvDestination);
      reporter1.setProperty("time_window_size", "2");
      reporter1.setProperty("decimal_format", "0.0");

      ReportManager rm = new ReportManager();
      rm.setProperty("tags", "http, gateway");
      rm.setProperty("uniqueId", "test");
      rm.loadConfigValues();
      rm.addReporter(reporter1);
      rm.assertUntouchedProperties();
      simulateIterations(rm);

      CsvFile csvFile = new CsvFile(TEST_OUTPUT_DIR + "/test_" + MeasurementTypes.AI_TOTAL + "_" + LabelTypes.ITERATIONS + ".csv");
      Assert.assertEquals(11, csvFile.getLines().size());
   }

   /**
    * Classical seconds/minutes/hours/days.
    */
   @Test
   public void time() {
      Reporter reporter1 = new ATReporter();
      CsvDestination csvDestination = new CsvDestination();
      csvDestination.setProperty("outputPath", TEST_OUTPUT_DIR);
      csvDestination.setProperty("periodicity", "2s");

      reporter1.addDestination(csvDestination);
      reporter1.setProperty("time_window_size", "2");
      reporter1.setProperty("decimal_format", "0.0");

      ReportManager rm = new ReportManager();
      rm.setProperty("tags", "http, gateway");
      rm.setProperty("uniqueId", "test");
      rm.loadConfigValues();
      rm.addReporter(reporter1);
      rm.assertUntouchedProperties();
      simulateIterations(rm);

      CsvFile csvFile = new CsvFile(TEST_OUTPUT_DIR + "/test_" + MeasurementTypes.AI_TOTAL + "_" + LabelTypes.ITERATIONS + ".csv");
      Assert.assertEquals(4, csvFile.getLines().size());
   }
}
