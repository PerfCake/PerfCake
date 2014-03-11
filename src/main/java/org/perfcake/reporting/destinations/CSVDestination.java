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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeConst;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.Utils;

/**
 * The destination that appends the {@link Measurement} into a CSV file.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class CSVDestination implements Destination {

   /**
    * Output CSV file path.
    */
   private String path = "perfcake-results-" + System.getProperty(PerfCakeConst.TIMESTAMP_PROPERTY) + ".csv";

   /**
    * Output CSV file.
    */
   private File csvFile = null;

   /**
    * CSV delimiter.
    */
   private String delimiter = ";";

   /**
    * The destination's logger.
    */
   private static final Logger log = Logger.getLogger(CSVDestination.class);

   /**
    * The list containing names of results from measurement.
    */
   private final List<String> resultNames = new ArrayList<>();

   /**
    * Cached headers in the CSV file
    */
   private String fileHeaders = null;

   /**
    * Strategy that is used in case that the output file, that this destination represents
    * was used by a different destination or scenario run before.
    **/
   private AppendStrategy appendStrategy = AppendStrategy.RENAME;

   /**
    * Strategy that is used in case that the output file exists. {@link AppendStrategy#OVERWRITE} means that the file
    * is overwritten, {@link AppendStrategy#RENAME} means that the current output file is renamed by adding a number-based
    * suffix and {@link AppendStrategy#FORCE_APPEND} is for appending new results to the original file.
    **/
   public enum AppendStrategy {
      /**
       * The original file is overwritten.
       **/
      OVERWRITE,

      /**
       * The original file is left alone but the output file is renamed according to a number-based pattern.
       **/
      RENAME,

      /**
       * The measurements are appended to the original file.
       **/
      FORCE_APPEND;
   }

   @Override
   public void open() {
      synchronized (this) {
         csvFile = new File(path);
         if (csvFile.exists()) {
            switch (appendStrategy) {
               case RENAME:
                  String name = csvFile.getAbsolutePath();
                  File f = null;
                  int ind = 1;
                  do {
                     f = new File(name + "." + (ind++));
                  } while (f.exists());
                  csvFile = f;
                  break;
               case OVERWRITE:
                  csvFile.delete();
                  break;
               case FORCE_APPEND:
               default:
                  // nothing to do here
            }
         }
      }
      if (log.isDebugEnabled()) {
         log.debug("Output path: " + csvFile.getAbsolutePath());
      }
   }

   @Override
   public void close() {
      // nothing to do
   }

   private void presetResultNames(final Measurement m) {
      final Map<String, Object> results = m.getAll();

      for (String key : results.keySet()) {
         if (!key.equals(Measurement.DEFAULT_RESULT)) {
            resultNames.add(key);
         }
      }
   }

   private String getFileHeaders(final Measurement m) {
      final StringBuilder sb = new StringBuilder();
      final Object defaultResult = m.get();

      sb.append("Time");
      sb.append(delimiter);
      sb.append("Iterations");
      if (defaultResult != null) {
         sb.append(delimiter);
         sb.append(Measurement.DEFAULT_RESULT);
      }
      for (String key : resultNames) {
         sb.append(delimiter);
         sb.append(key);
      }

      return sb.toString();
   }

   private String getResultsLine(final Measurement m) {
      final Object defaultResult = m.get();
      final Map<String, Object> results = m.getAll();
      final StringBuilder sb = new StringBuilder();

      sb.append(Utils.timeToHMS(m.getTime()));
      sb.append(delimiter);
      sb.append(m.getIteration() + 1);

      if (defaultResult != null) {
         sb.append(delimiter);
         if (defaultResult instanceof Quantity<?>) {
            sb.append(((Quantity<?>) defaultResult).getNumber());
         } else {
            sb.append(defaultResult);
         }
      }

      Object currentResult;
      for (String resultName : resultNames) {
         sb.append(delimiter);
         currentResult = results.get(resultName);
         if (currentResult instanceof Quantity<?>) {
            sb.append(((Quantity<?>) currentResult).getNumber());
         } else {
            sb.append(currentResult);
         }
      }

      return sb.toString();
   }

   @Override
   public void report(final Measurement m) throws ReportingException {
      // make sure the order of columns is consistent
      if (resultNames.isEmpty()) { // performance optimization before we enter the sync. block
         synchronized (this) {
            if (resultNames.isEmpty()) { // make sure the array did not get initialized while we were entering the sync. block
               presetResultNames(m);
               fileHeaders = getFileHeaders(m);
            }
         }
      }

      final String resultLine = getResultsLine(m);

      synchronized (this) {
         final boolean csvFileExists = csvFile.exists();
         try (FileOutputStream fos = new FileOutputStream(csvFile, true); OutputStreamWriter osw = new OutputStreamWriter(fos, Utils.getDefaultEncoding()); BufferedWriter bw = new BufferedWriter(osw)) {
            if (!csvFileExists) {
               bw.append(fileHeaders);
               bw.newLine();
            }
            bw.append(resultLine);
            bw.newLine();
         } catch (IOException ioe) {
            throw new ReportingException("Could not append a report to the file: " + csvFile.getPath(), ioe);
         }
      }

   }

   /**
    * Used to read the value of path.
    * 
    * @return The path value.
    */
   public String getPath() {
      return path;
   }

   /**
    * Used to set the value of path.
    * 
    * @param path
    *           The path value to set.
    */
   public void setPath(final String path) {
      this.path = path;
      this.csvFile = new File(this.path);
   }

   /**
    * Used to read the value of delimiter.
    * 
    * @return The delimiter value.
    */
   public String getDelimiter() {
      return delimiter;
   }

   /**
    * Used to set the value of delimiter.
    * 
    * @param delimiter
    *           The delimiter value to set.
    */
   public void setDelimiter(final String delimiter) {
      this.delimiter = delimiter;
   }

   /**
    * Used to read the value of appendStrategy.
    * 
    * @return The appendStrategy value.
    */
   public AppendStrategy getAppendStrategy() {
      return appendStrategy;
   }

   /**
    * Used to set the value of appendStrategy.
    * 
    * @param appendStrategy
    *           The appendStrategy value to set.
    */
   public void setAppendStrategy(AppendStrategy appendStrategy) {
      this.appendStrategy = appendStrategy;
   }
}
