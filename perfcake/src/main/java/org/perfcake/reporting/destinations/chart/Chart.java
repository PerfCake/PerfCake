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
package org.perfcake.reporting.destinations.chart;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.StringUtil;
import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Represents a single Google chart data file(s) stored in the file system.
 * Charts once read from a description file cannot be further modified and stored again!
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Chart {

   /**
    * Prefix of the files of the charts created as a combination of measured results.
    */
   protected static final String DATA_ARRAY_PREFIX = "data_array_";

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
   private static final Logger log = LogManager.getLogger(Chart.class);

   /**
    * File counter for the stored combined chart.
    */
   private static int fileCounter = 1;

   /**
    * Base of the file name of the chart file. E.g. from '/some/path/data/stats201501272232.js' it is just 'stats201501272232'.
    */
   private final String baseName;

   /**
    * The JavaScript file representing chart data. This is not set for charts created as a combination of existing ones.
    */
   private File dataFile;

   /**
    * A file channel for storing results.
    */
   private FileChannel outputChannel;

   /**
    * Target path for storing all data files related to this chart. These are the data itself (.js), the description file (.dat),
    * and the quick view file (.html).
    */
   private Path target;

   /**
    * Name of this chart.
    */
   private final String name;

   /**
    * The legend of the X axis of this chart.
    */
   private final String xAxis;

   /**
    * The legend of the Y axis of this chart.
    */
   private final String yAxis;

   /**
    * The type of the X axis. It can display the overall progress of the test in Percents, Time, or Iteration numbers.
    */
   private PeriodType xAxisType = PeriodType.TIME;

   /**
    * Attributes that should be stored from the Measurement.
    */
   private final List<String> attributes;

   /**
    * The chart's group name. Charts from multiple measurements that have the same group name are later searched for matching attributes.
    */
   private final String group;

   /**
    * Was this chart created as a concatenation of other charts?
    */
   private boolean concat = false;

   /**
    * This is set to false after a first result line is obtained from the getResultLine() method.
    * In the method, this is used to display a complete warning message for the user to be able to fix
    * the scenario. But we do not want the warning to show every time as it would slow down the performance.
    */
   private boolean firstResultsLine = true;

   /**
    * Create a new chart based on the information loaded from a description file.
    *
    * @param baseName
    *       Base of the data files.
    * @param group
    *       The name of the group the chart belongs to.
    * @param attributes
    *       Attributes stored for the chart.
    * @param name
    *       Name of this chart.
    * @param xAxis
    *       Legend of the X axis.
    * @param yAxis
    *       Legend of the Y axis.
    */
   private Chart(final String baseName, final String group, final List<String> attributes, final String name, final PeriodType xAxisType, final String xAxis, final String yAxis) {
      this.baseName = baseName;
      this.group = group;
      this.attributes = attributes;
      this.name = name;
      this.xAxisType = xAxisType;
      this.xAxis = xAxis;
      this.yAxis = yAxis;
   }

   /**
    * Create a new chart instance.
    *
    * @param target
    *       Target path where to store the data.
    * @param group
    *       Group of this chart.
    * @param attributes
    *       Attributes to be stored for this chart.
    * @param name
    *       Name of the chart.
    * @param xAxisType
    *       Type of the X axis - time, percents or iterations.
    * @param xAxis
    *       Legend of the X axis.
    * @param yAxis
    *       Legend of the Y axis.
    * @throws PerfCakeException
    *       When it was not possible to create necessary data files.
    */
   public Chart(final Path target, final String group, final List<String> attributes, final String name, final PeriodType xAxisType, final String xAxis, final String yAxis) throws PerfCakeException {
      this.target = target;
      this.attributes = attributes;
      this.name = name;
      this.xAxisType = xAxisType;
      this.xAxis = xAxis;
      this.yAxis = yAxis;
      this.group = group;

      baseName = group + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY);
      final Path dataFilePath = Paths.get(target.toString(), "data", baseName + ".js");
      dataFile = dataFilePath.toFile();

      writeDataFileHeader();
      writeDescriptionFile();
      writeQuickView();

      try {
         outputChannel = FileChannel.open(dataFilePath, StandardOpenOption.APPEND);
      } catch (final IOException e) {
         throw new PerfCakeException(String.format("Cannot open data file %s for appending data.", dataFile.getAbsolutePath()), e);
      }
   }

   /**
    * Gets a chart instance based on the given description file. The resulting instance will carry all the needed information but cannot
    * be later changed and stored again.
    *
    * @param descriptionFile
    *       The description file where to read the chart data from.
    * @return A new chart instance based on the information in the description file.
    * @throws IOException
    *       When it was not possible to read the data.
    */
   public static Chart fromDescriptionFile(final File descriptionFile) throws IOException {
      final String base = descriptionFile.getName().substring(0, descriptionFile.getName().length() - 4);
      final String group = base.substring(0, base.length() - 14);
      final String loaderEntry = new String(Files.readAllBytes(Paths.get(descriptionFile.toURI())), Utils.getDefaultEncoding());

      // drawChart(stats20150124220000, 'chart_stats20150124220000_div', [0, 1, 2], 'Time of test', 'Iterations per second', 'Performance');
      String name = loaderEntry.substring(loaderEntry.lastIndexOf(", ") + 3);
      name = name.substring(0, name.lastIndexOf("'"));

      String axises = loaderEntry.substring(loaderEntry.indexOf("], ") + 3, loaderEntry.lastIndexOf("', '"));
      final PeriodType xAxisType = PeriodType.values()[Integer.parseInt(axises.substring(0, axises.indexOf(",")))];
      axises = axises.substring(axises.indexOf("'") + 1);
      final String xAxis = axises.substring(0, axises.indexOf("', '"));
      final String yAxis = axises.substring(axises.indexOf("', '") + 4);

      final File jsFile = new File(descriptionFile.getAbsolutePath().substring(0, descriptionFile.getAbsolutePath().length() - 4) + ".js");
      String firstDataLine = "";
      try (BufferedReader br = Files.newBufferedReader(jsFile.toPath(), Charset.forName(Utils.getDefaultEncoding()));) {
         firstDataLine = br.readLine();
      }

      if (firstDataLine == null) {
         throw new IOException(String.format("Cannot read chart from file %s. No entries in the data file.", descriptionFile.getAbsolutePath()));
      }

      firstDataLine = firstDataLine.substring(firstDataLine.indexOf("[ [ ") + 4);
      firstDataLine = firstDataLine.substring(0, firstDataLine.indexOf(" ] ]"));
      final String[] columnNames = firstDataLine.split(", ");
      final List<String> columnsList = new ArrayList<>();
      for (final String s : columnNames) {
         columnsList.add(StringUtil.trim(s, "'"));
      }

      return new Chart(base, group, columnsList, name, xAxisType, xAxis, yAxis);
   }

   /**
    * Creates and saves a new chart as a combination of the given attribute and charts. The result is stored to the given path.
    *
    * @param target
    *       The path where to store the resulting chart.
    * @param match
    *       The name of the attribute that all the charts have in common. The resulting chart will display this attributed from all the charts.
    * @param charts
    *       The charts to be combined based on the given attribute.
    * @return The resulting chart representing the combination.
    * @throws PerfCakeException
    *       When it was not possible to store the created chart.
    */
   public static Chart combineCharts(final Path target, final String match, final List<Chart> charts) throws PerfCakeException {
      String base, baseFile;
      Path dataFile;
      do {
         base = DATA_ARRAY_PREFIX + (fileCounter++);
         baseFile = base + ".js";
         dataFile = Paths.get(target.toString(), "data", baseFile);
      } while (dataFile.toFile().exists());

      final List<String> columnNames = new ArrayList<>();
      switch (charts.get(0).getxAxisType()) {
         case PERCENTAGE:
            columnNames.add(COLUMN_PERCENT);
            break;
         case TIME:
            columnNames.add(COLUMN_TIME);
            break;
         case ITERATION:
            columnNames.add(COLUMN_ITERATION);
            break;
      }

      final StringBuilder baseNames = new StringBuilder();
      final StringBuilder columns = new StringBuilder();
      final StringBuilder lengths = new StringBuilder();
      final StringBuilder quotedNames = new StringBuilder();
      for (final Chart chart : charts) {
         if (baseNames.length() > 0) {
            baseNames.append(", ");
            columns.append(", ");
            lengths.append(", ");
            quotedNames.append(", ");
         }

         columnNames.add(chart.getBaseName());

         baseNames.append(chart.getBaseName());
         columns.append(chart.getAttributes().indexOf(match));
         lengths.append(chart.getBaseName());
         lengths.append(".length");
         quotedNames.append("'");
         quotedNames.append(chart.getName());
         quotedNames.append("'");
      }

      final Properties dataProps = new Properties();
      dataProps.setProperty("baseName", base);
      dataProps.setProperty("chartCols", columns.toString());
      dataProps.setProperty("chartLen", lengths.toString());
      dataProps.setProperty("chartsQuoted", quotedNames.toString());
      dataProps.setProperty("charts", baseNames.toString());
      Utils.copyTemplateFromResource("/charts/data-array.js", dataFile, dataProps);

      final Chart result = new Chart(base, charts.get(0).getGroup(), columnNames, "Group: " + charts.get(0).getGroup() + ", Match of axis: " + match, charts.get(0).getxAxisType(), charts.get(0).getxAxis(), charts.get(0).getyAxis());
      result.concat = true;

      return result;
   }

   /**
    * Gets the base name of the data files of this chart.
    *
    * @return The base name of the data files of this chart.
    */
   public String getBaseName() {
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
    * Writes the initial header and array definition to the JavaScript data file.
    *
    * @throws PerfCakeException
    *       When it was not possible to write the data.
    */
   private void writeDataFileHeader() throws PerfCakeException {
      final StringBuilder dataHeader = new StringBuilder("var ");
      dataHeader.append(baseName);
      dataHeader.append(" = [ [ ");
      boolean first = true;
      for (final String attr : attributes) {
         if (first) {
            dataHeader.append("'");
            first = false;
         } else {
            dataHeader.append(", '");
         }
         dataHeader.append(attr);
         dataHeader.append("'");
      }
      dataHeader.append(" ] ];\n");
      if (xAxisType == PeriodType.TIME) {
         dataHeader.append("var ho = (new Date(0)).getHours();\n"
               + "var offset = - (ho >= 12 ? ho - 24 : ho) * 60 * 60 * 1000;\n");
      }
      dataHeader.append("\n");
      Utils.writeFileContent(dataFile, dataHeader.toString());
   }

   /**
    * Writes a quick view HTML file that can display the chart during the test run.
    *
    * @throws PerfCakeException
    *       When it was not possible to store the quick view file.
    */
   private void writeQuickView() throws PerfCakeException {
      final Path quickViewFile = Paths.get(target.toString(), "data", baseName + ".html");
      final Properties quickViewProps = new Properties();
      quickViewProps.setProperty("baseName", baseName);
      quickViewProps.setProperty("loader", getLoaderLine());
      Utils.copyTemplateFromResource("/charts/quick-view.html", quickViewFile, quickViewProps);
   }

   /**
    * Writes a description file containing all needed information to draw the chart.
    *
    * @throws PerfCakeException
    *       When it was not possible to store the description file.
    */
   private void writeDescriptionFile() throws PerfCakeException {
      final Path instructionsFile = Paths.get(target.toString(), "data", baseName + ".dat");
      Utils.writeFileContent(instructionsFile, getLoaderLine());
   }

   /**
    * Gets the JavaScript instruction line that is used to draw the chart in the result report.
    * This is exactly what is stored in the description file too.
    *
    * @return The JavaScript code to draw the chart.
    */
   public String getLoaderLine() {
      final StringBuilder line = new StringBuilder("drawChart(");
      line.append(baseName);
      line.append(", 'chart_");
      line.append(baseName);
      line.append("_div', [0");

      for (int i = 1; i < attributes.size(); i++) {
         line.append(", ");
         line.append(i);
      }

      line.append("], ");
      line.append(xAxisType.ordinal());
      line.append(", '");
      line.append(xAxis);
      line.append("', '");
      line.append(yAxis);
      line.append("', '");
      line.append(name);
      line.append("');\n");

      return line.toString();
   }

   /**
    * Gets a JavaScript line to be written to the data file that represents the current Measurement.
    * All attributes required by the attributes list of this chart must be present in the measurement for the line to be returned.
    *
    * @param measurement
    *       The current measurement.
    * @return The line representing the data in measurement specified by the attributes list of this chart, or null when there was some of
    * the attributes missing.
    */
   private String getResultLine(final Measurement measurement) {
      final StringBuilder sb = new StringBuilder();
      boolean missingAttributes = false;

      sb.append(baseName);
      sb.append(".push([");
      switch (xAxisType) {
         case TIME:
            sb.append("new Date(");
            sb.append(measurement.getTime());
            sb.append(" + offset)");
            break;
         case ITERATION:
            sb.append(measurement.getIteration());
            break;
         case PERCENTAGE:
            sb.append(measurement.getPercentage());
            break;
      }

      for (final String attr : attributes) {
         if (attributes.indexOf(attr) > 0) {
            sb.append(", ");

            // we do not have all required attributes, return an empty line
            if (!measurement.getAll().containsKey(attr)) {
               missingAttributes = true;
               if (firstResultsLine) {
                  log.warn(String.format("Missing attribute %s, skipping the record.", attr));
               }
            } else {
               final Object data = measurement.get(attr);
               if (data instanceof String) {
                  sb.append("'");
                  sb.append((String) data);
                  sb.append("'");
               } else if (data instanceof Quantity) {
                  sb.append(((Quantity) data).getNumber().toString());
               } else {
                  sb.append(data == null ? "null" : data.toString());
               }
            }
         }
      }

      firstResultsLine = false;

      if (missingAttributes) { // we must postpone the return for all misses to be shown
         return "";
      }

      sb.append("]);\n");

      return sb.toString();
   }

   /**
    * Appends results to this chart based on the given Measurement.
    *
    * @param measurement
    *       The Measurement to be stored.
    * @throws ReportingException
    *       When it was not possible to write the data.
    */
   public void appendResult(final Measurement measurement) throws ReportingException {
      final String line = getResultLine(measurement);

      if (line != null && !"".equals(line)) {
         try {
            outputChannel.write(ByteBuffer.wrap(line.getBytes(Charset.forName(Utils.getDefaultEncoding()))));
         } catch (final IOException ioe) {
            throw new ReportingException(String.format("Could not append data to the chart file %s.", dataFile.getAbsolutePath()), ioe);
         }
      }
   }

   public void close() throws PerfCakeException {
      try {
         outputChannel.close();
      } catch (final IOException e) {
         throw new PerfCakeException(String.format("Cannot close output channel to the file %s.", dataFile.getAbsolutePath()), e);
      }
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
    * Was the chart created as a result of combining multiple other charts?
    *
    * @return True if and only if the chart was created as a combination of other charts.
    */
   public boolean isCombined() {
      return concat;
   }
}
