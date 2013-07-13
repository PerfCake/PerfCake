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

import java.text.DecimalFormat;

import org.perfcake.reporting.destinations.CsvDestination;
import org.perfcake.reporting.reporters.LabelTypes;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.reporting.reporters.ResponseTimeReporter;
import org.testng.annotations.Test;

public class ResponseTimeReporterTest extends ReportingTestBase {

   protected static final String DECIMAL_FORMAT_STRING = "0.0000";

   protected static final DecimalFormat decimalFormat = new DecimalFormat(DECIMAL_FORMAT_STRING);

   @Test
   public void basicResponseTime() throws ReportsException {
      Reporter reporter1 = new ResponseTimeReporter();
      CsvDestination csvDestination = new CsvDestination();
      csvDestination.setProperty("outputPath", TEST_OUTPUT_DIR);
      csvDestination.setProperty("periodicity", "1.0s");
      reporter1.addDestination(csvDestination);

      ReportManager rm = new ReportManager();
      rm.setProperty("tags", "http, gateway");
      rm.setProperty("uniqueId", "response");
      rm.loadConfigValues();
      rm.addReporter(reporter1);
      rm.assertUntouchedProperties();

      rm.reportTestStarted();
      rm.reportResponseTime(200);
      sleep(1.5f);
      rm.reportResponseTime(400);
      rm.reportResponseTime(100);
      rm.reportResponseTime(100);
      rm.reportTestFinished();

      assertCsvContainsExactly(TEST_OUTPUT_DIR + "/response_" + MeasurementTypes.AR + "_" + LabelTypes.PROCESSED_MESSAGES + ".csv", LabelTypes.PROCESSED_MESSAGES + ";" + MeasurementTypes.AR + "\n" + "1;" + decimalFormat.format(200) + "\n4;" + decimalFormat.format(200));
   }
}
