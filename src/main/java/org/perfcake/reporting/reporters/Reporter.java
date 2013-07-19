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

package org.perfcake.reporting.reporters;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.perfcake.reporting.ReportsException;
import org.perfcake.reporting.ScenarioConfigurationElement;
import org.perfcake.reporting.TestRunInfo;
import org.perfcake.reporting.destinations.Destination;

/**
 * <p>
 * Reporter is used to listen to generated performance measurements and transform them into reportable format. Then according to it's settings the reporter will report this data into those destinations (csv, database, ftp, console) and with specified periodicity.
 * </p>
 * <p/>
 * <table border="1">
 * <tr>
 * <td>Property name</td>
 * <td>Description</td>
 * <td>Required</td>
 * <td>Sample value</td>
 * <td>Default</td>
 * </tr>
 * <tr>
 * <td>decimal_format</td>
 * <td>Format of outputed decimal numbers</td>
 * <td>NO</td>
 * <td>0.00</td>
 * <td>0.0000</td>
 * </tr>
 * <tr>
 * <td>label_type</td>
 * <td>Label type that this reporter should use. Currently time and iteration are supported. If this is set to "default" then its up to specific reporter to choose labeling strategy.</td>
 * </td>
 * <td>YES</td>
 * <td>Iteration</td>
 * <td>default</td>
 * </tr>
 * </table>
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 */
public abstract class Reporter extends ScenarioConfigurationElement {
   /**
    * Three following booleans drive the way how the labeling is done.
    */
   protected boolean labelingTimely = false;

   protected boolean labelingIteration = false;

   protected boolean labelingDefault = false;

   private static final String PROP_DECIMAL_FORMAT = "decimal_format";

   /**
    * Formatter available for extension classes to use for formatting decimals
    * into string.
    */
   protected DecimalFormat decimalFormat = null;

   /**
    * All destinations into which this reporter will report.
    */
   protected List<Destination> destinations = new ArrayList<Destination>();

   private static final Logger log = Logger.getLogger(Reporter.class);

   protected TestRunInfo testRunInfo;

   private static final String PROP_LABEL_TYPE = "label_type";

   @Override
   public void loadConfigValues() throws ReportsException {
      decimalFormat = new DecimalFormat(getStringProperty(PROP_DECIMAL_FORMAT, "0.0000"));

      String labelType = getStringProperty(PROP_LABEL_TYPE, "default");
      if (labelType.toLowerCase().equals(LabelTypes.ITERATIONS.toLowerCase())) {
         labelingIteration = true;
      } else if (labelType.toLowerCase().equals(LabelTypes.TIME.toLowerCase())) {
         labelingTimely = true;
      } else if (labelType.toLowerCase().equals("default")) {
         labelingDefault = true;
      } else {
         throw new ReportsException("The label_type [" + labelType + "] must have one of the following value (case insensitive): " + LabelTypes.ITERATIONS + ", " + LabelTypes.TIME);
      }

      loadConfigVals();
   }

   public abstract void loadConfigVals() throws ReportsException;

   public abstract void periodicalTick(Destination dest) throws ReportsException;

   public void reportStart() throws ReportsException {
      for (Destination dest : destinations) {
         if (dest.isTimelyPeriodic()) {
            PeriodicalReportingThread periodicalThread = new PeriodicalReportingThread(this.getClass().getSimpleName(), dest);
            periodicalThread.start();
         }
      }

      testStarted();
   }

   public void reportEnd() throws ReportsException {
      for (Destination dest : destinations) {
         dest.stopThread();
      }
      testEnded();
   }

   public abstract void testStarted();

   public abstract void testEnded() throws ReportsException;

   public void setTestRunInfo(TestRunInfo trInfo) throws ReportsException {
      if (trInfo == null) {
         throw new ReportsException("Trying to set null for testRunInfo for reporter: " + this.getClass().getSimpleName());
      }

      this.testRunInfo = trInfo;

      for (Destination dest : destinations) {
         dest.setTestRunInfo(testRunInfo);
      }
   }

   public void addDestination(Destination dest) throws ReportsException {
      dest.loadConfigValues();
      dest.assertUntouchedProperties();
      dest.setReporter(this);
      destinations.add(dest);
      if (testRunInfo != null) {
         dest.setTestRunInfo(testRunInfo);
      }
   }

   public List<Destination> getDestinations() {
      return destinations;
   }

   public String getLabelType() throws ReportsException {
      return getLabelType("Label type choose error");
   }

   public String getLabel() throws ReportsException {
      return getLabel("Label type choose error");
   }

   /**
    * Gets label type that should be placed on value. If the reporter has set
    * fixed number of iterations or time the label type will be chosen
    * acordingly.
    * 
    * @param defaultVal
    * @return
    * @throws ReportsException
    */
   public String getLabelType(String defaultVal) throws ReportsException {
      if (!labelingTimely && !labelingIteration) {
         return defaultVal;
      }

      if (labelingIteration) {
         return LabelTypes.ITERATIONS;
      }

      if (labelingTimely) {
         return LabelTypes.TIME;
      }

      if (labelingDefault) {
         throw new ReportsException("Labeling is set to default. Please provide implementation in reporter to either set labelingIteration or provider default value directly into getLabelType method");
      }

      throw new ReportsException("The reporter is not configured to be either timely or iterationary");
   }

   /**
    * Gets label that should be placed on value. If the reporter has set fixed
    * number of iterations or time the label will be chosen acordingly.
    * 
    * @throws ReportsException
    */
   public String getLabel(String defaultVal) throws ReportsException {
      if (!labelingTimely && !labelingIteration) {
         return defaultVal;
      }

      if (labelingIteration) {
         return String.valueOf(testRunInfo.getProcessedIterations());
      }

      if (labelingTimely) {
         return testRunInfo.getTestRunTimeString();
      }

      if (labelingDefault) {
         throw new ReportsException("Labeling is set to default. Please provide implementation in reporter to either set labelingIteration or provider default value directly into getLabelType method");
      }

      throw new ReportsException("The reporter is not configured to be either timely or iterationary");

   }
}
