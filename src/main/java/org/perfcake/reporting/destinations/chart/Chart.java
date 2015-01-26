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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class Chart {
   private static final Logger log = LogManager.getLogger(Chart.class);

   private static final String DATA_ARRAY_PREFIX = "data_array_";

   private static int fileCounter = 1;

   private String baseName;

   private File dataFile;

   private Path target;

   private String name;

   private String xAxis;

   private String yAxis;

   private List<String> attributes;

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

   private void writeDataFileHeader() throws PerfCakeException {
      final StringBuilder dataHeader = new StringBuilder("var ");
      dataHeader.append(baseName);
      dataHeader.append(" = [ [ 'Time'");
      for (String attr : attributes) {
         dataHeader.append(", '");
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

      for (int i = 1; i <= attributes.size(); i++) {
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
