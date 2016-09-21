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
package org.perfcake.reporting.destination.c3chart;

import static org.perfcake.reporting.destination.ChartDestination.ChartType.LINE;

import org.perfcake.PerfCakeConst;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.destination.ChartDestination;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Represents a single C3 chart meta-data carrying all the information needed to represent the chart.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class C3Chart {

   /**
    * Name of the time column.
    */
   protected static final String COLUMN_TIME = "Time";

   /**
    * Name of the iteration column.
    */
   protected static final String COLUMN_ITERATION = "Iteration";

   /**
    * Name of the percentage column.
    */
   protected static final String COLUMN_PERCENT = "Percents";

   /**
    * A logger for the class.
    */
   private static final Logger log = LogManager.getLogger(C3Chart.class);

   /**
    * Base of the file name of the chart file. E.g. from '/some/path/data/stats201501272232.js' it is just 'stats201501272232'.
    */
   private String baseName;

   /**
    * Name of this chart.
    */
   private String name;

   /**
    * The legend of the X axis of this chart.
    */
   private String xAxis;

   /**
    * The legend of the Y axis of this chart.
    */
   private String yAxis;

   /**
    * The type of the X axis. It can display the overall progress of the test in Percents, Time, or Iteration numbers.
    */
   private PeriodType xAxisType = PeriodType.TIME;

   /**
    * Attributes that should be stored from the Measurement.
    */
   private List<String> attributes;

   /**
    * The chart's group name. Charts from multiple measurements that have the same group name are later searched for matching attributes.
    */
   private String group;

   /**
    * True when this chart was created as a combination of another charts.
    */
   private boolean combined = false;

   /**
    * When this chart was created.
    */
   private long created = System.currentTimeMillis();

   /**
    * Height of the resulting chart SVG graphics in pixels.
    */
   private int height = 400;

   /**
    * The chart can be either of line or bar type. Line is the default.
    */
   private ChartDestination.ChartType type = LINE;

   /**
    * Gets the base name of the data files of this chart.
    *
    * @return The base name of the data files of this chart.
    */
   public String getBaseName() {
      if (baseName == null) {
         baseName = group + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY);
      }

      return baseName;
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
    * Gets the legend of the X axis of the chart.
    *
    * @return The legend of the X axis of the chart.
    */
   public String getxAxis() {
      return xAxis;
   }

   /**
    * Gets the legend of the Y axis of the chart.
    *
    * @return The legend of the Y axis of the chart.
    */
   public String getyAxis() {
      return yAxis;
   }

   /**
    * Gets the attributes stored in the chart as a List.
    *
    * @return The attributes list.
    */
   public List<String> getAttributes() {
      return attributes;
   }

   /**
    * Gets the group of the current chart.
    *
    * @return The group name of this chart.
    */
   public String getGroup() {
      return group;
   }

   /**
    * Gets the type of the X axis. It can be either Time, Percents, or Iteration number.
    *
    * @return The type of the X axis.
    */
   public PeriodType getxAxisType() {
      return xAxisType;
   }

   /**
    * Sets the base of the file name of the chart file. E.g. from '/some/path/data/stats201501272232.js' it is just 'stats201501272232'.
    *
    * @param baseName
    *       The base of the file name of the chart file.
    */
   public void setBaseName(final String baseName) {
      this.baseName = baseName;
   }

   /**
    * Gets the name of the chart.
    *
    * @param name
    *       The name of the chart.
    */
   public void setName(final String name) {
      this.name = name;
   }

   /**
    * Sets the legend of the X axis of the chart.
    *
    * @param xAxis
    *       The legend of the X axis of the chart.
    */
   public void setxAxis(final String xAxis) {
      this.xAxis = xAxis;
   }

   /**
    * Sets the legend of the Y axis of the chart.
    *
    * @param yAxis
    *       The legend of the Y axis of the chart.
    */
   public void setyAxis(final String yAxis) {
      this.yAxis = yAxis;
   }

   /**
    * Gets the type of the X axis. It can be either Time, Percents, or Iteration number.
    *
    * @param xAxisType
    *       The type of the X axis. It can be either Time, Percents, or Iteration number.
    */
   public void setxAxisType(final PeriodType xAxisType) {
      this.xAxisType = xAxisType;
   }

   /**
    * Sets the attributes stored in the chart as a List.
    *
    * @param attributes
    *       The attributes stored in the chart as a List.
    */
   public void setAttributes(final List<String> attributes) {
      this.attributes = attributes;
   }

   /**
    * Sets the group of the current chart.
    *
    * @param group
    *       The group name of this chart.
    */
   public void setGroup(final String group) {
      this.group = group;
   }

   /**
    * Checks whether this chart was created as a combination of other existing charts. Charts created as a combination of other charts do not have
    * their object description and quick preview files generated.
    *
    * @return True if and only if this chart was created as a combination of other existing charts.
    */
   public boolean isCombined() {
      return combined;
   }

   /**
    * Sets whether this chart was created as a combination of other existing charts. Charts created as a combination of other charts do not have
    * their object description and quick preview files generated.
    *
    * @param combined
    *       True if and only if this chart was created as a combination of other existing charts.
    */
   public void setCombined(final boolean combined) {
      this.combined = combined;
   }

   /**
    * Gets the Unix timestamp of when this chart was created. Defaults to the class instantiation time.
    *
    * @return The Unix timestamp of when this chart was created.
    */
   public long getCreated() {
      return created;
   }

   /**
    * Sets the Unix timestamp of when this chart was created.
    *
    * @param created
    *       The Unix timestamp of when this chart was created.
    */
   public void setCreated(final long created) {
      this.created = created;
   }

   /**
    * Gets the height of the resulting chart SVG graphics in pixels.
    *
    * @return The height of the resulting chart SVG graphics in pixels.
    */
   public int getHeight() {
      return height;
   }

   /**
    * Sets the height of the resulting chart SVG graphics in pixels.
    *
    * @param height
    *       The height of the resulting chart SVG graphics in pixels.
    */
   public void setHeight(final int height) {
      this.height = height;
   }

   /**
    * Gets the chart's graphics type - either line or bar. Line is the default.
    *
    * @return The chart's graphics type.
    */
   public ChartDestination.ChartType getType() {
      return type;
   }

   /**
    * Sets the chart's graphics type - either line or bar. Line is the default.
    *
    * @param type
    *       The chart's graphics type.
    */
   public void setType(final ChartDestination.ChartType type) {
      this.type = type;
   }

   @Override
   public String toString() {
      return "C3Chart{"
            + "baseName='" + baseName + '\''
            + ", name='" + name + '\''
            + ", xAxis='" + xAxis + '\''
            + ", yAxis='" + yAxis + '\''
            + ", xAxisType=" + xAxisType
            + ", attributes=" + attributes
            + ", type=" + type
            + ", group='" + group
            + ", height=" + height + "'}";
   }
}
