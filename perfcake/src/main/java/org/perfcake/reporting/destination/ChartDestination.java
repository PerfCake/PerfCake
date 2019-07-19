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
package org.perfcake.reporting.destination;

import static org.perfcake.reporting.destination.ChartDestination.ChartType.LINE;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destination.c3chart.C3ChartHelper;
import org.perfcake.reporting.destination.util.DataBuffer;
import org.perfcake.util.properties.MandatoryProperty;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Creates nice charts from the results. The charts are generated to the outputPath path. The charts in the same group can be later
 * combined together based on the names of the columns. All previously generated charts in the outputPath path are placed in the final
 * report. Use with caution as the big number of results can take too long to load in the browser. Each chart has a quick view file
 * where the results can be seen while the test is still running.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ChartDestination extends AbstractDestination {

   /**
    * A logger for this class.
    */
   private static final Logger log = LogManager.getLogger(ChartDestination.class);

   /**
    * Attributes that should be stored in the chart.
    */
   @MandatoryProperty
   private List<String> attributes;

   /**
    * Where to store the charts.
    */
   private Path outputPath = Paths.get("perfcake-chart");

   /**
    * Name of the chart for this measurement.
    */
   private String name = "PerfCake Results";

   /**
    * Group of this chart. Charts in the same group can be later matched for the column names.
    */
   private String group = "default";

   /**
    * Y axis legend.
    */
   private String yAxis = "Value";

   /**
    * X axis legend.
    */
   private String xAxis = "Time";

   /**
    * The type of the X axis. It can display the overall progress of the test in Percents, Time, or Iteration numbers.
    */
   private PeriodType xAxisType = PeriodType.TIME;

   /**
    * Height of the resulting chart in pixels.
    */
   private int chartHeight = 400;

   /**
    * Helper to control the chart generation.
    */
   private C3ChartHelper helper;

   /**
    * True when we will automatically combine previous chart reports with the new one.
    */
   private boolean autoCombine = true;

   /**
    * Some attributes might end with an asterisk, in such a case, we are not able to create output until the end of the test.
    */
   private boolean dynamicAttributes = false;

   /**
    * The chart can be either of line or bar type. Line is the default.
    */
   private ChartType type = LINE;

   /**
    * Holds the data when the dynamic attributes are used and we cannot stream directly to a file.
    */
   private DataBuffer buffer;

   public enum ChartType {
      LINE, BAR;
   }

   @Override
   public void open() {
      dynamicAttributes = attributes.stream().anyMatch(s -> s.endsWith("*"));
      if (attributes.contains(PerfCakeConst.WARM_UP_TAG)) {
         attributes.remove(PerfCakeConst.WARM_UP_TAG);
         attributes.addAll(attributes.stream().map(attr -> attr + "_" + PerfCakeConst.WARM_UP_TAG).collect(Collectors.toList()));
      }

      if (dynamicAttributes) {
         buffer = new DataBuffer(attributes);
      } else {
         helper = new C3ChartHelper(this);
      }
   }

   @Override
   public void close() {
      if (dynamicAttributes) {
         attributes = buffer.getAttributes();
         helper = new C3ChartHelper(this);
      }

      if (!helper.isInitialized()) {
         log.error("Chart destination was not properly initialized, skipping result creation.");
      } else {
         try {
            if (dynamicAttributes) {
               buffer.replay((measurement) -> {
                  try {
                     helper.appendResult(measurement);
                  } catch (ReportingException e) {
                     log.error("Unable to write all reported data: ", e);
                  }
               });
            }

            helper.close();
            helper.compileResults(autoCombine);
         } catch (final PerfCakeException e) {
            log.error("Unable to compile all collected results in a final report: ", e);
         }
      }
   }

   @Override
   public void report(final Measurement measurement) throws ReportingException {
      if (dynamicAttributes) {
         buffer.record(measurement);
      } else {
         if (!helper.isInitialized()) {
            throw new ReportingException("Chart destination was not properly initialized.");
         }

         helper.appendResult(measurement);
      }
   }

   /**
    * Gets the output directory for charts.
    *
    * @return The output directory location.
    */
   public String getOutputDir() {
      return outputPath.toString();
   }

   /**
    * Sets the output directory for charts.
    *
    * @param outputDir
    *       The output directory location.
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setOutputDir(final String outputDir) {
      this.outputPath = Paths.get(outputDir);
      return this;
   }

   /**
    * Gets the output directory for charts as the Path data type.
    *
    * @return The output directory location.
    */
   public Path getOutputDirAsPath() {
      return outputPath;
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
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setName(final String name) {
      this.name = name;
      return this;
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
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setGroup(final String group) {
      if ("".equals(group)) {
         this.group = "default";
         log.warn("Empty group name, renaming to: default");
      } else if (!group.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
         final String postGroup = group.replaceAll("[^a-zA-Z0-9_]", "_");

         final Matcher firstNumericCharMatcher = Pattern.compile("[0-9]").matcher(postGroup);
         final int firstNumericCharIndex;
         if (firstNumericCharMatcher.find()) {
            firstNumericCharIndex = firstNumericCharMatcher.start();
         } else {
            firstNumericCharIndex = -1;
         }

         if (firstNumericCharIndex == 0) {
            this.group = "_" + postGroup;
         } else {
            this.group = postGroup;
         }
         log.warn("Illegal characters found in the group name. Only allowed characters are letters, numbers and underscore. The group must not start with a number. Renaming to: " + this.group);
      } else {
         this.group = group;
      }

      return this;
   }

   /**
    * Gets the height of the resulting chart in pixels.
    *
    * @return The height of the resulting chart in pixels.
    */
   public int getChartHeight() {
      return chartHeight;
   }

   /**
    * Sets the height of the resulting chart in pixels.
    *
    * @param chartHeight
    *       The height of the resulting chart in pixels.
    * @return The height of the resulting chart in pixels.
    */
   public ChartDestination setChartHeight(final int chartHeight) {
      this.chartHeight = chartHeight;
      return this;
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
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setyAxis(final String yAxis) {
      this.yAxis = yAxis;
      return this;
   }

   /**
    * Sets the legend of the Y axis of the chart.
    *
    * @param yAxis
    *       The legend of the Y axis of the chart.
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setYAxis(final String yAxis) {
      this.yAxis = yAxis;
      return this;
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
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setxAxis(final String xAxis) {
      this.xAxis = xAxis;
      return this;
   }

   /**
    * Sets the legend of the X axis of the chart.
    *
    * @param xAxis
    *       The legend of the X axis of the chart.
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setXAxis(final String xAxis) {
      this.xAxis = xAxis;
      return this;
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
    * Sets the type of the X axis. It can be either Time, Percents, or Iteration number.
    *
    * @param xAxisType
    *       The type of the X axis.
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setxAxisType(final PeriodType xAxisType) {
      this.xAxisType = xAxisType;
      return this;
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
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setAttributes(final String attributes) {
      this.attributes = new ArrayList<>(Arrays.asList(attributes.split("\\s*,\\s*")));
      return this;
   }

   /**
    * Gets the attributes that will be written to the chart as a List.
    *
    * @return The attributes list.
    */
   public List<String> getAttributesAsList() {
      return attributes;
   }

   /**
    * Should we automatically combine previous chart reports with the new one?
    *
    * @return True if and only if the combining feature is turned on.
    */
   public boolean isAutoCombine() {
      return autoCombine;
   }

   /**
    * Should we automatically combine previous chart reports with the new one?
    *
    * @param autoCombine
    *       True to turn the feature on, false to turn it off. Default is true/on.
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setAutoCombine(final boolean autoCombine) {
      this.autoCombine = autoCombine;
      return this;
   }

   /**
    * Gets the chart's graphics type - either line or bar. Line is the default.
    *
    * @return The chart's graphics type.
    */
   public ChartType getType() {
      return type;
   }

   /**
    * Sets the chart's graphics type - either line or bar. Line is the default.
    *
    * @param type
    *       The chart's graphics type.
    * @return Instance of this to support fluent API.
    */
   public ChartDestination setType(final ChartType type) {
      this.type = type;
      return this;
   }

}
