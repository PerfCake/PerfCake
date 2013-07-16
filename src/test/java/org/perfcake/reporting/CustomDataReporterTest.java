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
import org.perfcake.reporting.reporters.CustomReporter;
import org.perfcake.reporting.reporters.Reporter;
import org.testng.annotations.Test;

public class CustomDataReporterTest extends ReportingTestBase {

   @Test
   public void basic() throws ReportsException {
      Reporter customDataReporter = new CustomReporter();
      CsvDestination csvDestination = new CsvDestination();
      csvDestination.setOutputPath(TEST_OUTPUT_DIR);
      csvDestination.setPeriodicity("1s");

      customDataReporter.addDestination(csvDestination);

      ReportManager rm = new ReportManager();
      rm.setTags("http, gateway");
      rm.setUniqueId("custom");
      rm.loadConfigValues();
      rm.addReporter(customDataReporter);
      rm.assertUntouchedProperties();

      rm.reportTestStarted();
      rm.report(new Measurement("subtest01", "cm", "1", "44%"));
      sleep(1);
      rm.report(new Measurement("subtest02", "mm", "1", "ahoj"));
      rm.report(new Measurement("subtest01", "cm", "1", "44%"));
      rm.report(new Measurement("subtest02", "mm", "2", "ahoj2"));
      rm.report(new Measurement("subtest01", "cm", "2", "80%"));
      rm.report(new Measurement("subtest02", "mm", "3", "ahoj3"));
      rm.report(new Measurement("subtest01", "cm", "3", "100%"));
      rm.reportTestFinished();

      assertCsvContainsExactly(TEST_OUTPUT_DIR + "/custom_subtest01_cm.csv", "cm;subtest01\n" + "1;44%\n" + "2;80%\n" + "3;100%");

      assertCsvContainsExactly(TEST_OUTPUT_DIR + "/custom_subtest02_mm.csv", "mm;subtest02\n" + "1;ahoj\n" + "2;ahoj2\n" + "3;ahoj3");

      // Db design is not agreed upon yet
      // assertDatabaseContainsExactly("TEST_RESULTS",
      // "-1,1,2,2.00:");

   }
}
