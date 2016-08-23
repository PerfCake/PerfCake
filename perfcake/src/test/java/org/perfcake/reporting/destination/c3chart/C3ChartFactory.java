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

import org.perfcake.PerfCakeException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper factory class to be able to read chart data.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class C3ChartFactory {

   /**
    * Reads the chart meta-data from the directory structure, supposing there is only one chart stored. If there are more charts, random chart will be returned.
    *
    * @param basePath
    *       The base directory location.
    * @return The chart meta-data.
    * @throws PerfCakeException
    *       When it was not possible to read the data.
    */
   public static C3ChartDataFile readChartMetaData(final String basePath) throws PerfCakeException {
      return new C3ChartDataFile(new File(basePath + File.separator + "data", getBaseName(Paths.get(basePath), null) + ".json"));
   }

   /**
    * Reads the chart data from the directory structure, supposing there is only one chart stored. If there are more charts, random chart will be returned.
    *
    * @param basePath
    *       The base directory location.
    * @return The chart data.
    * @throws PerfCakeException
    *       When it was not possible to read the data.
    */
   public static C3ChartData readChartData(final String basePath) throws PerfCakeException {
      return new C3ChartData(getBaseName(Paths.get(basePath), null), Paths.get(basePath));
   }

   /**
    * Derive the chart base name from the path. Works only for directories with a single chart.
    *
    * @param tempPath
    *       Path to the directory with the chart.
    * @return The chart's base name.
    */
   private static String getBaseName(final Path tempPath, final String content) {
      String baseName = tempPath.resolve("data").toFile().list((dir, name) -> (content == null || "".equals(content) || name.contains(content)) && name.endsWith(".json"))[0];
      baseName = baseName.substring(0, baseName.lastIndexOf("."));

      return baseName;
   }

}
