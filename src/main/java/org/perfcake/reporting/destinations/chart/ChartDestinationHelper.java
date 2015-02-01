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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Helper class for the ChartDestination. Handles all files and data manipulation to keep the destination small and clear.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ChartDestinationHelper {

   /**
    * A logger for this class.
    */
   private static final Logger log = LogManager.getLogger(ChartDestinationHelper.class);

   /**
    * Main chart used to stored results of the parent ChartDestination.
    */
   private Chart mainChart;

   /**
    * Path specifying the location of the resulting charts.
    */
   private Path target;

   /**
    * Is the helper properly initialized without an exception? We cannot proceed on storing any data when this has failed.
    */
   private boolean successInit = false;

   /**
    * Creates a new helper for the given ChartDestination.
    *
    * @param chartDestination
    *       The ChartDestination this helper is supposed to serve to.
    */
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

   /**
    * Creates output files structure including all needed CSS and JS files.
    *
    * @throws PerfCakeException
    *       When it was not possible to create any of the directories or files.
    */
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
         Files.copy(getClass().getResourceAsStream("/charts/google-chart-concat.js"), Paths.get(target.toString(), "src", "google-chart-concat.js"), StandardCopyOption.REPLACE_EXISTING);
         Files.copy(getClass().getResourceAsStream("/charts/report.css"), Paths.get(target.toString(), "src", "report.css"), StandardCopyOption.REPLACE_EXISTING);
         Files.copy(getClass().getResourceAsStream("/charts/report.js"), Paths.get(target.toString(), "src", "report.js"), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
         throw new PerfCakeException("Cannot copy necessary chart resources to the output path: ", e);
      }
   }

   /**
    * Generates the JavaScript code to load all the charts in the provided list.
    *
    * @param charts
    *       Charts for which we want to generate the loader code.
    * @return A string representing the loader code.
    */
   private String getLoadersHtml(final List<Chart> charts) {
      final StringBuilder sb = new StringBuilder();
      for (final Chart chart : charts) {
         sb.append("         ");
         sb.append(chart.getLoaderLine());
      }
      return sb.toString();
   }

   /**
    * Creates JavaScript imports to load all the data files of the given charts.
    *
    * @param charts
    *       Charts that we want to be loaded in the resulting report.
    * @return A string representing the piece of HTML that loads the JavaScript data files.
    */
   private String getJsHtml(final List<Chart> charts) {
      final StringBuilder sb = new StringBuilder();
      for (final Chart chart : charts) {
         sb.append("      <script type=\"text/javascript\" src=\"data/");
         sb.append(chart.getBaseName());
         sb.append(".js\"></script>\n");
      }
      return sb.toString();
   }

   /**
    * Generates an HTML code with 'div' tags that will be placeholders for the given charts.
    *
    * @param charts
    *       Charts for which we want to generate the placeholders.
    * @return A string representing the piece of HTML with the 'div' placeholders.
    */
   private String getDivHtml(final List<Chart> charts) {
      final StringBuilder sb = new StringBuilder();
      for (final Chart chart : charts) {
         sb.append("     <div class=\"bs-docs-section clearfix\">\n"
               + "        <div class=\"row\">\n"
               + "          <div class=\"col-lg-12\">\n"
               + "            <div class=\"page-header\">\n"
               + "              <h1>");
         sb.append(chart.getName());
         sb.append("</h1>\n"
               + "            </div>\n"
               + "          </div>\n"
               + "        </div>\n"
               + "\n"
               + "        <div class=\"row\">\n"
               + "          <div class=\"col-lg-12\">\n"
               + "            <div class=\"bs-component\">\n"
               + "              <div id=\"chart_\">");
         sb.append(chart.getBaseName());
         sb.append("</div>\n"
               + "            </div>\n"
               + "\n"
               + "          </div>\n"
               + "        </div>\n"
               + "      </div>\n");
      }
      return sb.toString();
   }

   /**
    * Writes the master HTML index file.
    *
    * @param loaders
    *       A string representing the loader code.
    * @param js
    *       A string representing the piece of HTML that loads the JavaScript data files.
    * @param div
    *       A string representing the piece of HTML with the 'div' placeholders.
    * @throws PerfCakeException
    *       When it was not possible to generate the index file.
    */
   private void writeIndex(final String loaders, final String js, final String div) throws PerfCakeException {
      final Path indexFile = Paths.get(target.toString(), "index.html");
      final Properties indexProps = new Properties();
      indexProps.setProperty("js", js);
      indexProps.setProperty("loader", loaders);
      indexProps.setProperty("chart", div);
      Utils.copyTemplateFromResource("/charts/index.html", indexFile, indexProps);
   }

   /**
    * Find all attributes among the given charts that has a match. I.e. that are present at least in two of the charts.
    *
    * @param charts
    *       The charts for inspection.
    * @return A list of attributes that are present at least twice among the charts.
    */
   private Map<String, List<String>> findMatchingAttributes(final List<Chart> charts) {
      final Map<String, List<String>> seen = new HashMap<>();
      final Map<String, List<String>> result = new HashMap<>();

      for (Chart c : charts) {
         if (!seen.containsKey(c.getGroup())) {
            seen.put(c.getGroup(), new ArrayList<String>());
         }
         if (!result.containsKey(c.getGroup())) {
            result.put(c.getGroup(), new ArrayList<String>());
         }

         for (String attribute : c.getAttributes()) {
            if (seen.get(c.getGroup()).contains(attribute) && !result.get(c.getGroup()).contains(attribute)) {
               result.get(c.getGroup()).add(attribute);
            } else {
               seen.get(c.getGroup()).add(attribute);
            }
         }

         result.get(c.getGroup()).remove("Time");
         result.get(c.getGroup()).remove("Iteration");
      }


      return result;
   }

   /**
    * Generates new charts based on the matching attributes of existing charts.
    *
    * @param charts
    *       Existing chart for inspection.
    * @return A list of newly created charts.
    * @throws PerfCakeException
    *       When it was not possible to store any of the charts.
    */
   private List<Chart> analyzeMatchingCharts(final List<Chart> charts) throws PerfCakeException {
      final Map<String, List<String>> matches = findMatchingAttributes(charts);
      final List<Chart> newCharts = new ArrayList<>();

      for (String group : matches.keySet()) {
         for (String match : matches.get(group)) {
            final List<Chart> matchingCharts = new ArrayList<>();
            for (Chart c : charts) {
               if (c.getAttributes().contains(match)) {
                  matchingCharts.add(c);
               }
            }

            newCharts.add(Chart.combineCharts(target, match, matchingCharts));
         }
      }

      return newCharts;
   }

   /**
    * Deletes all previsouly generated chart combinations. We are going to refresh them.
    *
    * @param descriptionsDirectory
    *       The directory with existing generated charts.
    * @throws IOException
    *       When it was not possible to delete any of the charts.
    */
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
    * Creates the final report including generation of the main index.html file based on all previously generated reports in the same directory.
    *
    * @throws org.perfcake.PerfCakeException
    *       When it was not possible to generate the report.
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

   /**
    * Appends the results in the current Measurement to the main chart.
    *
    * @param m
    *       The current measurement.
    * @throws ReportingException
    *       When it was not possible to append the results.
    */
   public void appendResult(final Measurement m) throws ReportingException {
      mainChart.appendResult(m);
   }

   /**
    * Is the helper properly initialized?
    *
    * @return True if and only if the helper was properly initialized.
    */
   public boolean isSuccessInit() {
      return successInit;
   }

   /**
    * A file filter for description files.
    */
   private static class DescriptionFileFilter implements FileFilter {

      @Override
      public boolean accept(final File pathname) {
         return pathname.getName().toLowerCase().endsWith(".dat");
      }
   }

   /**
    * A file filter for chart files created as a combination of existing charts.
    */
   private static class CombinedJsFileFilter implements FileFilter {

      @Override
      public boolean accept(final File pathname) {
         return pathname.getName().toLowerCase().endsWith(".js") && pathname.getName().startsWith(Chart.DATA_ARRAY_PREFIX);
      }
   }

}
