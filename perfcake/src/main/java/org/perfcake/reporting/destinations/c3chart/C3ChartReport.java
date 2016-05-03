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
import org.perfcake.common.PeriodType;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compiles the resulting chart report combiting all possible charts created now or during previous runs in the same target location.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class C3ChartReport {

   /**
    * Prefix of the files of the charts created as a combination of measured results.
    */
   private static final String COMBINED_PREFIX = "combined_";

   /**
    * File counter for the stored combined chart.
    */
   private static int fileCounter = 1;

   /**
    * Creates the final report in the given location.
    *
    * @param target
    *       Root path to an existing chart report.
    * @param mainChart
    *       The chart that was added during this run of the performance test.
    * @throws PerfCakeException
    *       When it was not possible to create the report.
    */
   static void createReport(final Path target, final C3Chart mainChart) throws PerfCakeException {
      final File outputDir = Paths.get(target.toString(), "data").toFile();
      final List<C3Chart> charts = new ArrayList<>();
      charts.add(mainChart);

      try {
         deletePreviousCombinedCharts(outputDir);

         final File[] files = outputDir.listFiles(new DescriptionFileFilter());
         if (files != null) {
            final List<File> descriptionFiles = Arrays.asList(files);

            for (final File f : descriptionFiles) {
               final C3Chart c = new C3ChartDataFile(f).getChart();
               if (!c.getBaseName().equals(mainChart.getBaseName())) {
                  charts.add(c);
               }
            }
         }

         charts.sort(Comparator.comparingLong(C3Chart::getCreated));

         charts.addAll(analyzeMatchingCharts(target, charts));
      } catch (final IOException e) {
         throw new PerfCakeException("Unable to parse stored results: ", e);
      }

      C3ChartHtmlTemplates.writeIndex(target, charts);
   }

   /**
    * Deletes all previously generated chart combinations. We are going to refresh them.
    *
    * @param descriptionsDirectory
    *       The directory with existing generated charts.
    * @throws IOException
    *       When it was not possible to delete any of the charts.
    */
   private static void deletePreviousCombinedCharts(final File descriptionsDirectory) throws IOException {
      final StringBuilder issues = new StringBuilder();

      final File[] files = descriptionsDirectory.listFiles(new CombinedJsFileFilter());
      if (files != null) {
         for (final File f : files) {
            if (!f.delete()) {
               issues.append(String.format("Cannot delete file %s. %n", f.getAbsolutePath()));
            }
         }
      }

      if (issues.length() > 0) {
         throw new IOException(issues.toString());
      }
   }

   /**
    * Finds all the attributes among the given charts that has a match. I.e. that are present at least in two of the charts.
    *
    * @param charts
    *       The charts for inspection.
    * @return A map of chart group to a list of attributes that are present at least twice among the charts.
    */
   private static Map<String, List<String>> findMatchingAttributes(final List<C3Chart> charts) {
      final Map<String, List<String>> seen = new HashMap<>();
      final Map<String, List<String>> result = new HashMap<>();

      for (final C3Chart c : charts) {
         if (!seen.containsKey(c.getGroup())) {
            seen.put(c.getGroup(), new ArrayList<>());
         }
         if (!result.containsKey(c.getGroup())) {
            result.put(c.getGroup(), new ArrayList<>());
         }

         for (final String attribute : c.getAttributes()) {
            if (seen.get(c.getGroup()).contains(attribute) && !result.get(c.getGroup()).contains(attribute)) {
               result.get(c.getGroup()).add(attribute);
            } else {
               seen.get(c.getGroup()).add(attribute);
            }
         }

         result.get(c.getGroup()).remove(C3Chart.COLUMN_TIME);
         result.get(c.getGroup()).remove(C3Chart.COLUMN_ITERATION);
         result.get(c.getGroup()).remove(C3Chart.COLUMN_PERCENT);
      }

      return result;
   }

   /**
    * Generates new charts based on the matching attributes of existing charts.
    *
    * @param charts
    *       Existing chart for inspection.
    * @return The list of newly created charts.
    * @throws PerfCakeException
    *       When it was not possible to store any of the charts.
    */
   static List<C3Chart> analyzeMatchingCharts(final Path target, final List<C3Chart> charts) throws PerfCakeException {
      final Map<String, List<String>> matches = findMatchingAttributes(charts);
      final List<C3Chart> newCharts = new ArrayList<>();

      for (final Map.Entry<String, List<String>> entry : matches.entrySet()) {
         for (final String match : entry.getValue()) {
            final List<C3Chart> matchingCharts = new ArrayList<>();
            PeriodType xAxisType = null;
            boolean compatible = true; // all charts have compatible xAxisType

            for (final C3Chart c : charts) {
               if (entry.getKey().equals(c.getGroup()) && c.getAttributes().contains(match)) {
                  if (xAxisType == null) {
                     xAxisType = c.getxAxisType();
                     matchingCharts.add(c);
                  } else if (c.getxAxisType() == xAxisType) {
                     matchingCharts.add(c);
                  } else {
                     compatible = false;
                  }
               }
            }

            if (compatible) { // there are charts with different xAxisType, we won't combine them
               newCharts.add(combineCharts(target, match, matchingCharts));
            }
         }
      }

      return newCharts;
   }

   /**
    * Combines the charts in the target path according to the matching attribute. That means a new chart containing
    * the given attribute from all the charts generated in the given location is generated.
    *
    * @param target
    *       Root path to an existing chart report.
    * @param matchingAttribute
    *       The name of the attribute present in all of the charts.
    * @param matchingCharts
    *       The charts to be combined.
    * @return The newly created chart meta-data.
    * @throws PerfCakeException
    *       When it was not possible to write the new chart data.
    */
   private static C3Chart combineCharts(final Path target, final String matchingAttribute, final List<C3Chart> matchingCharts) throws PerfCakeException {
      final C3Chart newChart = new C3Chart();
      final String newBaseName = matchingCharts.stream().map(C3Chart::getBaseName).collect(Collectors.joining("_"));
      final List<String> attributes = new ArrayList<>();
      attributes.add(matchingCharts.get(0).getAttributes().get(0));
      attributes.addAll(matchingCharts.stream().map(ch -> String.format("%s (%s)", ch.getName(), C3ChartHtmlTemplates.getCreatedAsString(ch))).collect(Collectors.toList()));

      newChart.setBaseName(COMBINED_PREFIX + fileCounter++);
      newChart.setxAxisType(matchingCharts.get(0).getxAxisType());
      newChart.setxAxis(matchingCharts.get(0).getxAxis());
      newChart.setyAxis(matchingCharts.get(0).getyAxis());
      newChart.setName("Group: " + matchingCharts.get(0).getGroup() + ", Match of axis: " + matchingAttribute);
      newChart.setGroup(matchingCharts.get(0).getGroup());
      newChart.setAttributes(attributes);

      C3ChartData chartData = null;
      for (final C3Chart chart : matchingCharts) {
         C3ChartData tmpChartData = new C3ChartData(chart.getBaseName(), target);
         tmpChartData = tmpChartData.filter(chart.getAttributes().indexOf(matchingAttribute));

         if (chartData == null) {
            chartData = tmpChartData;
         } else {
            chartData = chartData.combineWith(tmpChartData);
         }
      }

      new C3ChartDataFile(newChart, target, chartData);

      return newChart;
   }

   /**
    * A file filter for description files.
    */
   private static class DescriptionFileFilter implements FileFilter {

      @Override
      public boolean accept(final File pathname) {
         return pathname.getName().toLowerCase().endsWith(".json");
      }
   }

   /**
    * A file filter for chart files created as a combination of existing charts.
    */
   private static class CombinedJsFileFilter implements FileFilter {

      @Override
      public boolean accept(final File pathname) {
         return pathname.getName().toLowerCase().endsWith(".js") && pathname.getName().startsWith(COMBINED_PREFIX);
      }
   }

}
