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
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.Utils;
import org.perfcake.validation.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Represents a single Google chart data file(s) stored in the file system.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class Chart {
   private static final Logger log = LogManager.getLogger(Chart.class);

   protected static final String DATA_ARRAY_PREFIX = "data_array_";

   private static int fileCounter = 1;

   private String baseName;

   private File dataFile;

   private Path target;

   private String name;
   private String xAxis;
   private String yAxis;
   private List<String> attributes;

   private Chart(final String baseName, final List<String> attributes, final String name, final String xAxis, final String yAxis) {
      this.baseName = baseName;
      this.attributes = attributes;
      this.name = name;
      this.xAxis = xAxis;
      this.yAxis = yAxis;
   }

   public Chart(final Path target, final String group, final List<String> attributes, final String name, final String xAxis, final String yAxis) throws PerfCakeException {
      this.target = target;
      this.attributes = attributes;
      this.name = name;
      this.xAxis = xAxis;
      this.yAxis = yAxis;

      baseName = group + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY);
      dataFile = Paths.get(target.toString(), "data", baseName + ".js").toFile();

      writeDataFileHeader();
      writeDescriptionFile();
      writeQuickView();
   }

   public static Chart fromDescriptionFile(final File descriptionFile) throws IOException {
      final String base = descriptionFile.getName().substring(0, descriptionFile.getName().length() - 4);
      final String loaderEntry = new String(Files.readAllBytes(Paths.get(descriptionFile.toURI())));

      // drawChart(stats20150124220000, 'chart_stats20150124220000_div', [0, 1, 2], 'Time of test', 'Iterations per second', 'Performance');
      String name = loaderEntry.substring(loaderEntry.lastIndexOf(", ") + 3);
      name = name.substring(0, name.lastIndexOf("'"));

      String axises = loaderEntry.substring(loaderEntry.indexOf("], '") + 4, loaderEntry.lastIndexOf("', '"));
      String xAxis = axises.substring(0, axises.indexOf("', '"));
      String yAxis = axises.substring(axises.indexOf("', '") + 4);

      final File jsFile = new File(descriptionFile.getAbsolutePath().substring(0, descriptionFile.getAbsolutePath().length() - 4) + ".js");
      String firstDataLine = "";
      try (BufferedReader br = Files.newBufferedReader(jsFile.toPath(), Charset.forName(Utils.getDefaultEncoding()));) {
         firstDataLine = br.readLine();
      }

      firstDataLine = firstDataLine.substring(firstDataLine.indexOf("[ [ ") + 4);
      firstDataLine = firstDataLine.substring(0, firstDataLine.indexOf(" ] ]"));
      String[] columnNames = firstDataLine.split(", ");
      List<String> columnsList = new ArrayList<>();
      for (String s : columnNames) {
         columnsList.add(StringUtil.trim(s, "'"));
      }

      return new Chart(base, columnsList, name, xAxis, yAxis);
   }

   public static Chart combineCharts(final Path target, final String match, final List<Chart> charts) throws PerfCakeException {
      String base, baseFile;
      Path dataFile;
      do {
         base = DATA_ARRAY_PREFIX + (fileCounter++);
         baseFile = base + ".js";
         dataFile = Paths.get(target.toString(), "data", baseFile);
      } while (dataFile.toFile().exists());

      final List<String> columnNames = new ArrayList<>();
      columnNames.add("Time");
      final StringBuilder baseNames = new StringBuilder();
      final StringBuilder columns = new StringBuilder();
      final StringBuilder lengths = new StringBuilder();
      final StringBuilder quotedNames = new StringBuilder();
      for (Chart chart : charts) {
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
         quotedNames.append(chart.getBaseName());
         quotedNames.append("'");
      }

      final Properties dataProps = new Properties();
      dataProps.setProperty("baseName", base);
      dataProps.setProperty("chartCols", columns.toString());
      dataProps.setProperty("chartLen", lengths.toString());
      dataProps.setProperty("chartsQuoted", quotedNames.toString());
      dataProps.setProperty("charts", baseNames.toString());
      Utils.copyTemplateFromResource("/charts/data-array.js", dataFile, dataProps);

      return new Chart(base, columnNames, "Match of " + match, charts.get(0).getxAxis(), charts.get(0).getyAxis());
   }

   public String getBaseName() {
      return baseName;
   }

   public String getName() {
      return name;
   }

   public String getxAxis() {
      return xAxis;
   }

   public String getyAxis() {
      return yAxis;
   }

   public List<String> getAttributes() {
      return attributes;
   }

   private void writeDataFileHeader() throws PerfCakeException {
      final StringBuilder dataHeader = new StringBuilder("var ");
      dataHeader.append(baseName);
      dataHeader.append(" = [ [ ");
      boolean first = true;
      for (String attr : attributes) {
         if (first) {
            dataHeader.append("'");
            first = false;
         } else {
            dataHeader.append(", '");
         }
         dataHeader.append(attr);
         dataHeader.append("'");
      }
      dataHeader.append(" ] ];\n\n");
      Utils.writeFileContent(dataFile, dataHeader.toString());
   }

   private void writeQuickView() throws PerfCakeException {
      final Path quickViewFile = Paths.get(target.toString(), "data", baseName + ".html");
      final Properties quickViewProps = new Properties();
      quickViewProps.setProperty("baseName", baseName);
      quickViewProps.setProperty("loader", getLoaderLine());
      Utils.copyTemplateFromResource("/charts/quick-view.html", quickViewFile, quickViewProps);
   }

   private void writeDescriptionFile() throws PerfCakeException {
      final Path instructionsFile = Paths.get(target.toString(), "data", baseName + ".dat");
      Utils.writeFileContent(instructionsFile, getLoaderLine());
   }

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

      line.append("], '");
      line.append(xAxis);
      line.append("', '");
      line.append(yAxis);
      line.append("', '");
      line.append(name);
      line.append("');\n");

      return line.toString();
   }

   private String getResultLine(final Measurement m) {
      StringBuilder sb = new StringBuilder();
      sb.append(baseName);
      sb.append(".push(['");
      sb.append(Utils.timeToHMS(m.getTime()));
      sb.append("'");

      for (String attr : attributes) {
         if (!attr.equals("Time")) {
            sb.append(", ");
            Object data = m.get(attr);

            // we do not have all required attributes, return an empty line
            if (data == null) {
               return "";
            }

            if (data instanceof String) {
               sb.append("'");
               sb.append((String) data);
               sb.append("'");
            } else {
               sb.append(data.toString());
            }
         }
      }

      sb.append("]);\n");

      return sb.toString();
   }

   public void appendResult(final Measurement m) throws ReportingException {
      String line = getResultLine(m);

      if (line != null && !"".equals(line)) {
         try (FileOutputStream fos = new FileOutputStream(dataFile, true); OutputStreamWriter osw = new OutputStreamWriter(fos, Utils.getDefaultEncoding()); BufferedWriter bw = new BufferedWriter(osw)) {
            bw.append(line);
         } catch (IOException ioe) {
            throw new ReportingException(String.format("Could not append data to the chart file %s.", dataFile.getAbsolutePath()), ioe);
         }
      }
   }

}
