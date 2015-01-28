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

import org.perfcake.PerfCakeException;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.chart.ChartDestinationHelper;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Creates nice charts from the results. The charts are generated to the target path. The charts in the same group can be later
 * combined together based on the names of the columns. All previously generated charts in the target path are placed in the final
 * report. Use with caution as the big number of results can take too long to load in the browser. Each chart has a quick view file
 * where the results can be seen while the test is still running.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ChartDestination implements Destination {

   /**
    * A logger for this class.
    */
   private static final Logger log = LogManager.getLogger(ChartDestination.class);

   /**
    * Attributes that should be stored in the chart.
    */
   private List<String> attributes;

   /**
    * Where to store the charts.
    */
   private Path target = Paths.get("perfcake-chart");

   /**
    * Name of the chart for this measurement.
    */
   private String name = "PerfCake Results";

   /**
    * Group of this chart. Charts in the same group can be later matched for the column names.
    */
   private String group = "results";

   /**
    * Y axis legend.
    */
   private String yAxis = "Iterations";

   /**
    * X axis legend.
    */
   private String xAxis = "Time";

   /**
    * Helper to control the chart generation.
    */
   private ChartDestinationHelper helper;

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#open()
    */
   @Override
   public void open() {
      helper = new ChartDestinationHelper(this);
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#close()
    */
   @Override
   public void close() {
      if (!helper.isSuccessInit()) {
         log.error("Chart destination was not properly initialized, skipping result creation.");
      } else {
         try {
            helper.compileResults();
         } catch (PerfCakeException e) {
            log.error("Unable to compile all collected results in a final report: ", e);
         }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#report(org.perfcake.reporting.Measurement)
    */
   @Override
   public void report(final Measurement m) throws ReportingException {
      if (!helper.isSuccessInit()) {
         throw new ReportingException("Chart destination was not properly initialized.");
      }

      helper.appendResult(m);
   }

   /**
    * Gets the target output directory for charts.
    *
    * @return The target location.
    */
   public String getTarget() {
      return target.toString();
   }

   /**
    * Sets the target output directory for charts.
    *
    * @param target
    *       The target location.
    */
   public void setTarget(final String target) {
      this.target = Paths.get(target);
   }

   /**
    * Gets the target output directory for charts as the Path data type.
    *
    * @return The target location.
    */
   public Path getTargetAsPath() {
      return target;
   }

   /**
    * Gets the name of the chart.
    *
    * @return The name of the chart.
    */
   public String getName() {
      return name;
   }

   /**
    * Sets the name of the chart.
    *
    * @param name
    *       The name of the chart.
    */
   public void setName(final String name) {
      this.name = name;
   }

   /**
    * Gets the group of the resulting chart.
    *
    * @return The group of the chart.
    */
   public String getGroup() {
      return group;
   }

   /**
    * Sets the group of the resulting chart.
    *
    * @param group
    *       The group of the chart.
    */
   public void setGroup(final String group) {
      this.group = group;
   }

   /**
    * Gets the legend of the Y axis of the chart.
    *
    * @return The legend of the Y axis of the chart.
    */
   public String getYAxis() {
      return yAxis;
   }

   /**
    * Sets the legend of the Y axis of the chart.
    *
    * @param yAxis
    *       The legend of the Y axis of the chart.
    */
   public void setYAxis(final String yAxis) {
      this.yAxis = yAxis;
   }

   /**
    * Gets the legend of the X axis of the chart.
    *
    * @return The legend of the X axis of the chart.
    */
   public String getXAxis() {
      return xAxis;
   }

   /**
    * Sets the legend of the X axis of the chart.
    *
    * @param xAxis
    *       The legend of the X axis of the chart.
    */
   public void setXAxis(final String xAxis) {
      this.xAxis = xAxis;
   }

   /**
    * Gets the attributes that will be written to the chart.
    *
    * @return The attributes separated by comma.
    */
   public String getAttributes() {
      return StringUtils.join(attributes, ",");
   }

   /**
    * Sets the attributes that will be written to the chart.
    *
    * @param attributes
    *       The attributes separated by comma.
    */
   public void setAttributes(final String attributes) {
      this.attributes = Arrays.asList(attributes.split("\\s*,\\s*"));
   }

   /**
    * Gets the attributes that will be written to the chart as a List.
    *
    * @return The attributes list.
    */
   public List<String> getAttributesAsList() {
      return attributes;
   }
}
