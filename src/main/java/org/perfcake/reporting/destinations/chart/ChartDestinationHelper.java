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

import org.perfcake.PerfCakeException;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.ChartDestination;
import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Helper class for the ChartDestination. Handles all files and data manipulation to keep the destination small and clear.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ChartDestinationHelper {

   private static final Logger log = LogManager.getLogger(ChartDestinationHelper.class);

   private Chart mainChart;

   private Path target;

   private boolean successInit = false;

   public ChartDestinationHelper(final ChartDestination chartDestination) {
      target = chartDestination.getTargetAsPath();

      try {
         createOutputFileStructure();

         List<String> attributes = new ArrayList<>(chartDestination.getAttributesAsList()); // must close to ArrayList, as the current impl. does not support adding at index
         attributes.add(0, "Time");

         mainChart = new Chart(target, chartDestination.getGroup(), attributes, chartDestination.getName(), chartDestination.getXAxis(), chartDestination.getYAxis());

         successInit = true;
      } catch (PerfCakeException e) {
         log.error(String.format("%s did not get initialized properly:", this.getClass().getName()), e);
         successInit = false;
      }
   }

   private void createOutputFileStructure() throws PerfCakeException {
      if (!target.toFile().exists()) {
         if (!target.toFile().mkdirs()) {
            throw new PerfCakeException("Could not create output directory: " + target.toFile().getAbsolutePath());
         }
      } else {
         if (!target.toFile().isDirectory()) {
            throw new PerfCakeException("Could not create output directory. It already exists as a file: " + target.toFile().getAbsolutePath());
         }
      }

      File dir = Paths.get(target.toString(), "data").toFile();
      if (!dir.exists() && !dir.mkdirs()) {
         throw new PerfCakeException("Could not create data directory: " + dir.getAbsolutePath());
      }

      dir = Paths.get(target.toString(), "src").toFile();
      if (!dir.exists() && !dir.mkdirs()) {
         throw new PerfCakeException("Could not create source directory: " + dir.getAbsolutePath());
      }

      try {
         Files.copy(getClass().getResourceAsStream("/charts/google-chart.js"), Paths.get(target.toString(), "src", "google-chart.js"), StandardCopyOption.REPLACE_EXISTING);
         Files.copy(getClass().getResourceAsStream("/charts/google-jsapi.js"), Paths.get(target.toString(), "src", "google-jsapi.js"), StandardCopyOption.REPLACE_EXISTING);
         Files.copy(getClass().getResourceAsStream("/charts/jquery.js"), Paths.get(target.toString(), "src", "jquery.js"), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
         throw new PerfCakeException("Cannot copy necessary chart resources to the output path: ", e);
      }
   }

   private String getLoadersHtml(final List<Chart> charts) {
      final StringBuilder sb = new StringBuilder();
      for (final Chart chart : charts) {
         sb.append("         ");
         sb.append(chart.getLoaderLine());
      }
      return sb.toString();
   }

   private String getJsHtml(final List<Chart> charts) {
      final StringBuilder sb = new StringBuilder();
      for (final Chart chart : charts) {
         sb.append("      <script type=\"text/javascript\" src=\"data/");
         sb.append(chart.getBaseName());
         sb.append(".js\"></script>\n");
      }
      return sb.toString();
   }

   private String getDivHtml(final List<Chart> charts) {
      final StringBuilder sb = new StringBuilder();
      for (final Chart chart : charts) {
         sb.append("      <div id=\"chart_");
         sb.append(chart.getBaseName());
         sb.append("_div\"></div>\n");
      }
      return sb.toString();
   }

   private void writeIndex(final String loaders, final String js, final String div) throws PerfCakeException {
      final Path indexFile = Paths.get(target.toString(), "index.html");
      final Properties indexProps = new Properties();
      indexProps.setProperty("js", js);
      indexProps.setProperty("loader", loaders);
      indexProps.setProperty("chart", div);
      Utils.copyTemplateFromResource("/charts/index.html", indexFile, indexProps);
   }

   private List<String> findMatchingAttributes(final List<Chart> charts) {
      final List<String> seen = new ArrayList<>();
      final List<String> result = new ArrayList<>();

      for (Chart c : charts) {
         for (String attribute : c.getAttributes()) {
            if (seen.contains(attribute) && !result.contains(attribute)) {
               result.add(attribute);
            } else {
               seen.add(attribute);
            }
         }
      }

      result.remove("Time");
      result.remove("Iteration");

      return result;
   }

   // return new charts based on matches
   private List<Chart> analyzeMatchingCharts(final List<Chart> charts) throws PerfCakeException {
      final List<String> matches = findMatchingAttributes(charts);
      final List<Chart> newCharts = new ArrayList<>();

      for (String match : matches) {
         final List<Chart> matchingCharts = new ArrayList<>();
         for (Chart c : charts) {
            if (c.getAttributes().contains(match)) {
               matchingCharts.add(c);
            }
         }

         newCharts.add(Chart.combineCharts(target, match, matchingCharts));
      }

      return newCharts;
   }

   private void deletePreviousCombinedCharts(final File descriptionsDirectory) throws IOException {
      final StringBuilder issues = new StringBuilder();

      for (File f : descriptionsDirectory.listFiles(new CombinedJsFileFilter())) {
         if (!f.delete()) {
            issues.append(String.format("Cannot delete file %s. \n", f.getAbsolutePath()));
         }
      }

      if (issues.length() > 0) {
         throw new IOException(issues.toString());
      }
   }

   /**
    * Creates the main index.html file based on all previously generated reports in the same directory.
    *
    * @throws java.io.IOException
    */
   public void compileResults() throws PerfCakeException {
      final File outputDir = Paths.get(target.toString(), "data").toFile();
      final List<Chart> charts = new ArrayList<>();
      charts.add(mainChart);

      try {
         deletePreviousCombinedCharts(outputDir);

         final List<File> descriptionFiles = Arrays.asList(outputDir.listFiles(new DescriptionFileFilter()));

         for (final File f : descriptionFiles) {
            Chart c = Chart.fromDescriptionFile(f);
            if (!c.getBaseName().equals(mainChart.getBaseName())) {
               charts.add(c);
            }
         }

         charts.addAll(analyzeMatchingCharts(charts));
      } catch (IOException e) {
         throw new PerfCakeException("Unable to parse stored results: ", e);
      }

      writeIndex(getLoadersHtml(charts), getJsHtml(charts), getDivHtml(charts));
   }

   public void appendResult(final Measurement m) throws ReportingException {
      mainChart.appendResult(m);
   }

   public boolean isSuccessInit() {
      return successInit;
   }

   private static class DescriptionFileFilter implements FileFilter {

      @Override
      public boolean accept(final File pathname) {
         return pathname.getName().toLowerCase().endsWith(".dat");
      }
   }

   private static class CombinedJsFileFilter implements FileFilter {

      @Override
      public boolean accept(final File pathname) {
         return pathname.getName().toLowerCase().endsWith(".js") && pathname.getName().startsWith(Chart.DATA_ARRAY_PREFIX);
      }
   }

}
