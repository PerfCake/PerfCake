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

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.StringTemplate;
import org.perfcake.util.Utils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Creates nice charts from the results.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ChartDestination implements Destination {

   private static final Logger log = Logger.getLogger(ChartDestination.class);

   private List<String> attributes;

   private Path target = Paths.get("perfcake-chart");

   private String name = "PerfCake Results";

   private String group = "results";

   private String yAxis = "Iterations";

   private String xAxis = "Time";

   private String baseName;

   private File dataFile;

   private boolean successInit = false;

   private boolean createOutputFileStructure() {
      if (!target.toFile().exists()) {
         if (!target.toFile().mkdirs()) {
            log.error("Could not create output directory: " + target.toFile().getAbsolutePath());
            return false;
         }
      } else {
         if (!target.toFile().isDirectory()) {
            log.error("Could not create output directory. It already exists as a file: " + target.toFile().getAbsolutePath());
            return false;
         }
      }

      File dir = Paths.get(target.toString(), "data").toFile();
      if (!dir.mkdirs()) {
         log.error("Could not create data directory: " + dir.getAbsolutePath());
         return false;
      }

      dir = Paths.get(target.toString(), "src").toFile();
      if (!dir.mkdirs()) {
         log.error("Could not create source directory: " + dir.getAbsolutePath());
         return false;
      }

      try {
         Files.copy(getClass().getResourceAsStream("/charts/google-chart.js"), Paths.get(target.toString(), "src", "google-chart.js"));
      } catch (IOException e) {
         log.error("Cannot copy necessary chart resources to the output path: ", e);
         return false;
      }

      return true;
   }

   private boolean writeDataFileHeader() {
      final StringBuilder dataHeader = new StringBuilder("var ");
      dataHeader.append(baseName);
      dataHeader.append(" = [ [ 'Time'");
      for (String attr : attributes) {
         dataHeader.append(", '");
         dataHeader.append(attr);
         dataHeader.append("'");
      }
      dataHeader.append("] ];\n\n");
      try {
         Files.write(dataFile.toPath(), dataHeader.toString().getBytes(Utils.getDefaultEncoding()));
      } catch (IOException e) {
         log.error(String.format("Cannot write the data header to the chart data file %s: ", dataFile.getAbsolutePath()), e);
         return false;
      }

      return true;
   }

   private String writeChartDatFile() {
      final Path instructionsFile = Paths.get(target.toString(), "data", baseName + ".dat");
      final StringBuilder instructions = new StringBuilder("drawChart(");
      instructions.append(baseName);
      instructions.append(", 'chart_");
      instructions.append(baseName);
      instructions.append("_div', [0");
      for (int i = 1; i <= attributes.size(); i++) {
         instructions.append(", ");
         instructions.append(i);
      }
      instructions.append("], '");
      instructions.append(xAxis);
      instructions.append("', '");
      instructions.append(yAxis);
      instructions.append("', '");
      instructions.append(name);
      instructions.append("');\n");
      try {
         Files.write(instructionsFile, instructions.toString().getBytes(Utils.getDefaultEncoding()));
      } catch (IOException e) {
         log.error(String.format("Cannot write instructions file %s: ", instructionsFile.toFile().getAbsolutePath()), e);
         return null;
      }

      return instructions.toString();
   }

   private boolean writeQuickView(final String loaderLine) {
      final Path quickViewFile = Paths.get(target.toString(), "data", baseName + ".html");
      final Properties quickViewProps = new Properties();
      quickViewProps.setProperty("baseName", baseName);
      quickViewProps.setProperty("loader", loaderLine);
      try {
         StringTemplate quickView = new StringTemplate(new String(Files.readAllBytes(Paths.get(Utils.getResourceAsUrl("/charts/quick-view.html").toURI())), Utils.getDefaultEncoding()), quickViewProps);
         Files.write(quickViewFile, quickView.toString().getBytes(Utils.getDefaultEncoding()));
      } catch (IOException | PerfCakeException | URISyntaxException e) {
         log.error("Cannot store quick view file: ", e);
         return false;
      }

      return true;
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

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#open()
    */
   @Override
   public void open() {
      successInit = createOutputFileStructure();

      if (successInit) {
         baseName = group + System.getProperty(PerfCakeConst.TIMESTAMP_PROPERTY);
         dataFile = Paths.get(target.toString(), "data", baseName + ".js").toFile();

         // write beginning of the data file
         successInit = writeDataFileHeader(); // successInit was already true

         // write .dat file for later use by loader
         final String loaderLine = writeChartDatFile();
         successInit = successInit && (loaderLine != null);

         // write a quick view file
         successInit = successInit && writeQuickView(loaderLine);
      }

      // nop
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#close()
    */
   @Override
   public void close() {
      // nop
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#report(org.perfcake.reporting.Measurement)
    */
   @Override
   public void report(final Measurement m) throws ReportingException {
      if (!successInit) {
         throw new ReportingException("Chart destination was not properly initialized.");
      }

      String line = getResultLine(m);

      if (line != null && !"".equals(line)) {
         try (FileOutputStream fos = new FileOutputStream(dataFile, true); OutputStreamWriter osw = new OutputStreamWriter(fos, Utils.getDefaultEncoding()); BufferedWriter bw = new BufferedWriter(osw)) {
            bw.append(line);
         } catch (IOException ioe) {
            throw new ReportingException(String.format("Could not append data to the chart file %s.", dataFile.getAbsolutePath()), ioe);
         }
      }
   }

   public String getTarget() {
      return target.toString();
   }

   public void setTarget(final String target) {
      this.target = Paths.get(target);
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getGroup() {
      return group;
   }

   public void setGroup(final String group) {
      this.group = group;
   }

   public String getYAxis() {
      return yAxis;
   }

   public void setYAxis(final String yAxis) {
      this.yAxis = yAxis;
   }

   public String getXAxis() {
      return xAxis;
   }

   public void setXAxis(final String xAxis) {
      this.xAxis = xAxis;
   }

   public String getAttributes() {
      return StringUtils.join(attributes, ",");
   }

   public void setAttributes(final String attributes) {
      this.attributes = Arrays.asList(StringUtils.split(attributes, ", "));
   }
}
