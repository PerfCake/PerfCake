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
import org.perfcake.reporting.destinations.chart.ChartDestinationHelper;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class C3ChartReport {

   /**
    * Prefix of the files of the charts created as a combination of measured results.
    */
   private static final String COMBINED_PREFIX = "combined_";


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

      //charts.addAll(analyzeMatchingCharts(charts));
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
   static void deletePreviousCombinedCharts(final File descriptionsDirectory) throws IOException {
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
