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
 * This unit tests verifies that it is not possible to report 2 meaurements for
 * one label into any destination. This was a bug and this test verifies
 * non-regresivity.
 */
public class NonOverlayReportingTest extends ReportingTestBase {

   /**
    * When percentage output is set to 10% there should be (obviously) 10
    * records in csv no more no less.
    * 
    * @throws ReportsException
    */
   @Test
   public void percentagic() throws ReportsException {

      Reporter reporter1 = new ATReporter();
      CsvDestination csvDestination = new CsvDestination();
      csvDestination.setOutputPath(TEST_OUTPUT_DIR);
      csvDestination.setPeriodicity("10%");
      reporter1.addDestination(csvDestination);
      reporter1.setProperty("decimal_format", "0.0");

      ReportManager rm = new ReportManager();
      rm.getTestRunInfo().setTestIterations(10);
      rm.setTags("http, gateway");
      rm.setUniqueId("test");
      rm.loadConfigValues();
      rm.addReporter(reporter1);
      rm.assertUntouchedProperties();
      simulateIterations(rm);

      CsvFile csvFile = new CsvFile(TEST_OUTPUT_DIR + "/test_" + MeasurementTypes.AI_TOTAL + "_" + LabelTypes.ITERATIONS + ".csv");
      // +1 for head
      Assert.assertEquals(11, csvFile.getLines().size());
   }

   /**
    * When percentage output is set to 10% there should be (obviously) 10
    * records in csv no more no less. This test (aditionaly to previous). uses
    * timely settings
    * 
    * @throws ReportsException
    */
   @Test
   public void percentagicTimely() throws ReportsException {

      Reporter reporter1 = new ATReporter();
      CsvDestination csvDestination = new CsvDestination();
      csvDestination.setOutputPath(TEST_OUTPUT_DIR);
      csvDestination.setPeriodicity("21%");
      reporter1.addDestination(csvDestination);
      reporter1.setProperty("decimal_format", "0.0");

      ReportManager rm = new ReportManager();
      rm.getTestRunInfo().setTestDuration(5);
      rm.setTags("http, gateway");
      rm.setUniqueId("test");
      rm.loadConfigValues();
      rm.addReporter(reporter1);
      rm.assertUntouchedProperties();
      simulateIterations(rm);

      CsvFile csvFile = new CsvFile(TEST_OUTPUT_DIR + "/test_" + MeasurementTypes.AI_TOTAL + "_" + LabelTypes.ITERATIONS + ".csv");
      // +1 for head
      Assert.assertEquals(5, csvFile.getLines().size());
   }
}
