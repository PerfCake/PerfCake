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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.perfcake.nreporting.MeasurementUnit;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.ATReporter;
import org.perfcake.reporting.reporters.CustomReporter;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.reporting.reporters.ResponseTimeReporter;

/**
 * <p>
 * Report manager has responsibility of collecting performance data and providing them to reporters.
 * </p>
 * <p>
 * There is always 1 instance of ReportManager for 1 test thus we can set tags/unique identifiers right into the ReportManagers properties.
 * </p>
 * <p>
 * Programmers should use report* methods to report about various events. .
 * </p>
 * <p>
 * To use reporting capabilities, programmer must call reportTestStarted() and at the end of the Test programmer has to call reportTestFinished()
 * </p>
 * 
 * <p>
 * Report manager allows following properties
 * <table border="1">
 * <tr>
 * <td>Property name</td>
 * <td>Description</td>
 * <td>Required</td>
 * <td>Sample value</td>
 * <td>Default</td>
 * </tr>
 * <tr>
 * <td>tags</td>
 * <td>Tags that describe the test properties</td>
 * <td>YES</td>
 * <td>testcomponent1, esb, java</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>uniqueId</td>
 * <td>Unique ID that will be used as a identifier of this test. To assure uniqueness the scenario file should define this id with aid of some variable</td>
 * <td>YES</td>
 * <td>TestHTTP_GW_${testnumber}</td>
 * <td></td>
 * </tr>
 * </table>
 * </p>
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class ReportManager extends ScenarioConfigurationElement {
   private static final Logger log = Logger.getLogger(ReportManager.class);

   private static final String PROP_UNIQ_ID = "uniqueId";

   private static final String PROP_TAGS = "tags";

   private String tags = "";
   private String uniqueId = null;

   /**
    * Information about running test case
    */
   private TestRunInfo testRunInfo = new TestRunInfo();

   private List<Reporter> reporters = new ArrayList<Reporter>();

   /**
    * Reporters
    */
   private ATReporter atreporter = null;

   private ResponseTimeReporter rtreporter = null;

   private CustomReporter measurementReporter = null;

   /**
    * Test case info should be provided to the ReportManager through property
    * elements in scenario XML, this method is called when parsing of these
    * elements is done and Report Manager thus can construct the TestRunInfo
    * 
    * @throws ReportsException
    */
   @Override
   public void loadConfigValues() throws ReportsException {
      TestRunInfo tci = getTestRunInfo();
      tci.addTags(splitCommaSeparatedProperty(tags));

      tci.setUniqueId(uniqueId);
      if (uniqueId == null || uniqueId.equals("")) {
         throw new ReportsException(PROP_UNIQ_ID + " property in scenario file has to be " + "nonempty string for ReportManager.");
      }
   }

   @Override
   public void assertUntouchedProperties() throws ReportsException {
      for (Reporter reporter : reporters) {
         reporter.assertUntouchedProperties();
      }
   }

   /**
    * Reports that test started. This method always has to be called before
    * starting the test.
    */
   public void reportTestStarted() {

      testRunInfo.setTestStartTime(System.currentTimeMillis());
      for (Reporter r : reporters) {
         try {
            r.reportStart();
         } catch (ReportsException e) {
            log.error("Unable to report test start: ", e);
         }
      }
   }

   /**
    * Reports that test finished. This method always has to be called after test
    * finishes. If this method isn't called then various problems will arise for
    * example test reports won't be commited to database or csv files!
    * 
    * @throws ReportsException
    */
   public void reportTestFinished() {
      if (!testRunInfo.testStarted()) {
         log.warn("Error while finishing test, nothing is going to be reported. The test didn't even started! Please call " + "reportTestStarted() before test starts.");
         return;
      }

      testRunInfo.setTestEndTime(System.currentTimeMillis());
      for (Reporter r : reporters) {
         try {
            r.reportEnd();
         } catch (ReportsException e) {
            log.error("Unable to report test end: ", e);
         }
      }

      if (log.isInfoEnabled()) {
         long iters = testRunInfo.getProcessedIterations();
         long duration = testRunInfo.getTestEndTime() - testRunInfo.getTestStartTime();
         log.info(iters + " iterations was processed in " + duration + " ms (" + 1000f * iters / duration + " iterations/s)");
      }
   }

   /**
    * Reports custom measurement.
    * 
    * @param measurement
    */
   public void report(Measurement measurement) {
      if (measurementReporter != null && testRunInfo.testStarted() && !testRunInfo.testFinished()) {
         try {
            measurementReporter.report(measurement);
         } catch (ReportsException e) {
            log.error("Unable to report measurement: ", e);
         }
      }
   }

   private Object reportIterationLock = new Object();

   /**
    * Call this method to report that iteration has been processed. This method
    * handles situations when report of iteration is made but the test has not
    * been yet started.
    * 
    * @throws ReportsException
    */
   public void reportIteration() throws ReportsException {
      synchronized (reportIterationLock) {
         if (testRunInfo.testStarted() && !testRunInfo.testFinished()) {
            long it = testRunInfo.incrementIteration();
            // When the iteration passes some destinations might hit the
            // treshold upon which they have to report.
            for (Reporter reporter : reporters) {
               for (Destination dest : reporter.getDestinations()) {
                  if (dest.isIterationaryPeriodic()) {
                     if (it % dest.getItTreshold() == 0) {
                        dest.periodicalTick();
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * Use this method to report that there was certain response time of certain
    * component.
    * 
    * @param time
    *           Response time in miliseconds
    */
   public void reportResponseTime(double time) {
      if (rtreporter != null && testRunInfo.testStarted() && !testRunInfo.testFinished()) {
         rtreporter.report(time);
      }
   }

   public void addReporter(Reporter reporter) throws ReportsException {
      if (testRunInfo == null) {
         throw new ReportsException("Cannot add reporter to report manager before TestCaseInfo is set. To set TestCase info please call constructTestCaseInfo() after the property elements of Report Manager are set.");
      }

      if (reporter instanceof ATReporter) {
         atreporter = (ATReporter) reporter;
      }
      if (reporter instanceof ResponseTimeReporter) {
         rtreporter = (ResponseTimeReporter) reporter;
      }
      if (reporter instanceof CustomReporter) {
         measurementReporter = (CustomReporter) reporter;
      }

      reporter.setTestRunInfo(testRunInfo);
      reporter.loadConfigValues();
      reporters.add(reporter);
   }

   public TestRunInfo getTestRunInfo() {
      if (testRunInfo == null) {
         testRunInfo = new TestRunInfo();
      }

      return testRunInfo;
   }

   public String getTags() {
      return tags;
   }

   public void setTags(String tags) {
      this.tags = tags;
   }

   public String getUniqueId() {
      return uniqueId;
   }

   public void setUniqueId(String uniqueId) {
      this.uniqueId = uniqueId;
   }

   public void reportIteration(MeasurementUnit mu) {
      // TODO Auto-generated method stub

   }

}
