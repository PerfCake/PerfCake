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

package org.perfcake.reporting.destinations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.NullPeriodicity;
import org.perfcake.reporting.Periodicity;
import org.perfcake.reporting.ReportsException;
import org.perfcake.reporting.ScenarioConfigurationElement;
import org.perfcake.reporting.TestRunInfo;
import org.perfcake.reporting.reporters.PeriodicalReportingThread;
import org.perfcake.reporting.reporters.Reporter;

/**
 * <p>
 * Destination is place where things are to be reported with reporter. Examples of such destinations are: database, csvfile, console.
 * </p>
 * 
 * <p>
 * We will report many different kinds of reports into one general data structure defined in conceptual model. The definition follows
 * </p>
 * 
 * <p>
 * 
 * <h2>Tables</h2>
 * <h3>TestRun</h3>
 * Concrete build in Jenkins. For this build results are collected.
 * 
 * <h3>Measurements</h3>
 * Measurements contain collection of measured values that represent output of the test. For example response time can be measured.
 * 
 * <h3>Measurement type</h3>
 * Type of the Measurement with regard to it's semantic that has been collected e.g. Response time, Iterations per second.
 * 
 * <h3>Label type</h3>
 * LabelType is type of labels for Measured values. Because Measurement can contain many Measured values then each value is labeled somehow. For example possible label types: Time, Processed messages, Message number
 * 
 * <h3>MeasuredValues</h3>
 * 00:02:33, 445
 * 
 * <h2>Example</h2>
 * Complete example could be as follows.
 * 
 * MeasurementTypes MT1 ResponseTime MT2 Iterations per second
 * 
 * Label types L1 Time L2 Processed messages
 * 
 * Measurements S1 MT1 L1
 * 
 * MeasuredValues S1 00:01:00 50ms S1 00:01:30 48ms S1 00:01:30 47ms S1 00:02:00 47ms
 * </p>
 * 
 * <p>
 * This destination can be set following properties:
 * <table border="1">
 * <tr>
 * <td>Property name</td>
 * <td>Description</td>
 * <td>Required</td>
 * <td>Sample value</td>
 * </tr>
 * <tr>
 * <td>periodicity</td>
 * <td>How often (in Periodicity string, see javadoc for Periodicity) should this reporter report to its destinations.</td>
 * <td>NO</td>
 * <td>10s</td>
 * </tr>
 * </table>
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public abstract class Destination extends ScenarioConfigurationElement {
   private static final Logger log = Logger.getLogger(Destination.class);

   private static final String PROP_PERIODICAL_INTERVAL = "periodicity";

   private Periodicity periodicity = null;

   protected TestRunInfo testRunInfo;

   private Reporter reporter;

   private PeriodicalReportingThread periodicalThread;

   protected ConcurrentLinkedQueue<Measurement> messageQueue = new ConcurrentLinkedQueue<Measurement>();

   @Override
   public void loadConfigValues() throws ReportsException {
      String pstring = getStringProperty(PROP_PERIODICAL_INTERVAL, "");

      if (pstring.equals("")) {
         periodicity = new NullPeriodicity();
      } else {
         periodicity = Periodicity.constructFromString(pstring);
      }

      loadSpecificConfigValues();
   }

   public abstract void send() throws ReportsException;

   /**
    * Measurement is pending to get outputed. This method shoudn't allow
    * duplicit measurements for same label.
    * 
    * @param measurement
    */
   private Map<String, Measurement> lastMeasurementsForTypes = new HashMap<String, Measurement>();

   public void addMessageToSendQueue(Measurement measurement) {

      if (lastMeasurementsForTypes.containsKey(measurement.getMeasurementType())) {
         Measurement lastMeasurement = lastMeasurementsForTypes.get(measurement.getMeasurementType());
         if (lastMeasurement.getMeasurementType().equals(measurement.getMeasurementType()) && lastMeasurement.getLabel().equals(measurement.getLabel())) {
            log.warn("There was measurement [" + measurement + "] that had same label as lastly added measurement [" + lastMeasurement + "]. This duplicit measurement will be discarded!");
            return;
         }
      }
      {
         lastMeasurementsForTypes.put(measurement.getMeasurementType(), measurement);
      }
      messageQueue.add(measurement);
      lastMeasurementsForTypes.remove(measurement.getMeasurementType());
      lastMeasurementsForTypes.put(measurement.getMeasurementType(), measurement);
   }

   public void addMessagesToSendQueue(List<Measurement> measurements) {
      for (Measurement m : measurements) {
         addMessageToSendQueue(m);
      }
   }

   public void stopThread() {
      if (periodicalThread != null) {
         periodicalThread.stopThread();
      }
   }

   public void setPeriodicalThread(PeriodicalReportingThread periodicalReportingThread) throws ReportsException {
      if (reporter == null) {
         throw new ReportsException("The destination " + this.getClass().getSimpleName() + " doesn't have reporter attached but is trying to be started as periodical! Nobody to report to!");
      }
      periodicalThread = periodicalReportingThread;
   }

   public void periodicalTick() throws ReportsException {
      reporter.periodicalTick(this);
   }

   public abstract void loadSpecificConfigValues() throws ReportsException;

   public Reporter getReporter() {
      return reporter;
   }

   public void setReporter(Reporter reporter) {
      this.reporter = reporter;
   }

   public void setTestRunInfo(TestRunInfo testRunInfo) {
      this.testRunInfo = testRunInfo;
      periodicity.setTestRunInfo(testRunInfo);
   }

   public TestRunInfo getTestCaseInfo() {
      return testRunInfo;
   }

   public boolean isTimelyPeriodic() throws ReportsException {
      return periodicity.isTimely();
   }

   public float getPeriodicalInterval() throws ReportsException {
      return periodicity.getTimePeriodicity();
   }

   public boolean isIterationaryPeriodic() throws ReportsException {
      return periodicity.isIterationary();
   }

   public int getItTreshold() throws ReportsException {
      return periodicity.getIterationalPeriodicity();
   }
}
