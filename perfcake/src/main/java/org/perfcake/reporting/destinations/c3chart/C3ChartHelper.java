/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package org.perfcake.reporting.destinations.c3chart;

import org.perfcake.PerfCakeException;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.ChartDestination;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for the ChartDestination. Bridges the destination methods to the corresponding actions of other classes in the package.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class C3ChartHelper {

   /**
    * A logger for this class.
    */
   private static final Logger log = LogManager.getLogger(C3ChartHelper.class);

   /**
    * Name of the time column.
    */
   static final String COLUMN_TIME = "Time";

   /**
    * Name of the iteration column.
    */
   static final String COLUMN_ITERATION = "Iteration";

   /**
    * Name of the percentage column.
    */
   static final String COLUMN_PERCENT = "Percents";

   /**
    * Main chart data file used to store results of the parent ChartDestination.
    */
   private C3ChartDataFile chartDataFile;

   /**
    * Is the helper properly initialized without an exception? We cannot proceed on storing any data when this has failed.
    */
   private boolean initialized = false;

   /**
    * Creates a new helper for the given ChartDestination.
    *
    * @param chartDestination
    *       The ChartDestination this helper is supposed to serve to.
    */
   public C3ChartHelper(final ChartDestination chartDestination) {
      try {
         final List<String> attributes = new ArrayList<>(chartDestination.getAttributesAsList()); // must be enclosed to ArrayList, as the current impl. does not support adding at index

         switch (chartDestination.getxAxisType()) {
            case PERCENTAGE:
               attributes.add(0, COLUMN_PERCENT);
               break;
            case TIME:
               attributes.add(0, COLUMN_TIME);
               break;
            case ITERATION:
               attributes.add(0, COLUMN_ITERATION);
               break;
         }

         final C3Chart chart = new C3Chart();
         chart.setGroup(chartDestination.getGroup());
         chart.setAttributes(attributes);
         chart.setName(chartDestination.getName());
         chart.setxAxisType(chartDestination.getxAxisType());
         chart.setxAxis(chartDestination.getxAxis());
         chart.setyAxis(chartDestination.getyAxis());
         chart.setHeight(chartDestination.getChartHeight());

         chartDataFile = new C3ChartDataFile(chart, chartDestination.getOutputDirAsPath());
         chartDataFile.open();

         initialized = true;
      } catch (final PerfCakeException e) {
         log.error(String.format("%s did not get initialized properly:", this.getClass().getName()), e);
         initialized = false;
      }
   }

   /**
    * Appends the results in the current Measurement to the main chart.
    *
    * @param measurement
    *       The current measurement.
    * @throws ReportingException
    *       When it was not possible to append the results.
    */
   public void appendResult(final Measurement measurement) throws ReportingException {
      chartDataFile.appendResult(measurement);
   }

   /**
    * Is the helper properly initialized?
    *
    * @return True if and only if the helper was properly initialized.
    */
   public boolean isInitialized() {
      return initialized;
   }

   /**
    * Closes the data files.
    *
    * @throws PerfCakeException
    *       When it was not possible to smoothly finalize the data files.
    */
   public void close() throws PerfCakeException {
      chartDataFile.close();
   }

   /**
    * Creates the final result report compiling all the previous charts together.
    *
    * @param autoCombine
    *       True if we should combine the new results with the previous reports.
    * @throws PerfCakeException
    *       When there was an error creating the report. Typically an I/O issue.
    */
   public void compileResults(final boolean autoCombine) throws PerfCakeException {
      C3ChartReport.createReport(chartDataFile.getTarget(), chartDataFile.getChart(), autoCombine);
   }

}
