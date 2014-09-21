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

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeConst;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The destination that appends the {@link Measurement} into a CSV file.
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class CsvDestination implements Destination {

   /**
    * Output CSV file path.
    */
   private String path = "perfcake-results-" + System.getProperty(PerfCakeConst.TIMESTAMP_PROPERTY) + ".csv";

   /**
    * Output CSV file.
    */
   private File csvFile = null;

   /**
    * CSV data elements delimiter.
    */
   private String delimiter = ";";

   /**
    * Logger.
    */
   private static final Logger log = Logger.getLogger(CsvDestination.class);

   /**
    * The list containing names of results from measurement.
    */
   private final List<String> resultNames = new ArrayList<>();

   /**
    * Cached headers in the CSV file.
    */
   private String fileHeaders = null;

   /**
    * Each line in the output will be prefixed with this string.
    */
   private String linePrefix = "";

   /**
    * Each line in the output will be suffixed with this string.
    */
   private String lineSuffix = "";

   /**
    * New output line delimiter.
    */
   private String lineBreak = "\n";

   /**
    * Skip writing header to the file.
    */
   private boolean skipHeader = false;

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
      FORCE_APPEND
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
                     //
                     int lastDot = name.lastIndexOf(".");
                     if (lastDot > -1) {
                        f = new File(name.substring(0, lastDot) + "." + (ind++) + name.substring(lastDot));
                     } else {
                        f = new File(name + "." + (ind++));
                     }
                  } while (f.exists());
                  csvFile = f;
                  break;
               case OVERWRITE:
                  if (!csvFile.delete()) {
                     log.warn(String.format("Unable to delete the file %s, forcing append.", csvFile.getAbsolutePath()));
                  }
                  break;
               case FORCE_APPEND:
               default:
                  // nothing to do here
            }
         }
      }
      if (log.isDebugEnabled()) {
         log.debug(String.format("Opened CSV destination to the file %s.", path));
      }
   }

   @Override
   public void close() {
      synchronized (this) {
         csvFile = null;
      }
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

      final StringBuilder sb = new StringBuilder();
      if (linePrefix != null && !linePrefix.isEmpty()) {
         sb.append(linePrefix);
      }
      sb.append(getResultsLine(m));
      if (lineSuffix != null && !lineSuffix.isEmpty()) {
         sb.append(lineSuffix);
      }
      sb.append(lineBreak);

      synchronized (this) {
         final boolean csvFileExists = csvFile.exists();
         try (FileOutputStream fos = new FileOutputStream(csvFile, true); OutputStreamWriter osw = new OutputStreamWriter(fos, Utils.getDefaultEncoding()); BufferedWriter bw = new BufferedWriter(osw)) {
            if (!csvFileExists && !skipHeader) {
               bw.append(fileHeaders);
               bw.append(lineBreak);
            }

            bw.append(sb.toString());
         } catch (IOException ioe) {
            throw new ReportingException(String.format("Could not append a report to the file %s.", csvFile.getPath()), ioe);
         }
      }

   }

   /**
    * Gets the currently used output file path.
    *
    * @return The current output file path.
    */
   public String getPath() {
      return path;
   }

   /**
    * Sets the output file path.
    * Once the destination opens the target file, the changes to this property are ignored.
    *
    * @param path
    *           The output file path to be set.
    */
   public CsvDestination setPath(final String path) {
      synchronized (this) {
         if (csvFile != null) {
            throw new UnsupportedOperationException("Changing the value of path after opening the destination is not allowed.");
         }
      }

      this.path = path;
      return this;
   }

   /**
    * Gets the line data elements delimiter.
    *
    * @return The data elements delimiter.
    */
   public String getDelimiter() {
      return delimiter;
   }

   /**
    * Sets the delimiter used in a line between individual data elements.
    *
    * @param delimiter
    *           The delimiter to be used between data elements in an output line.
    */
   public CsvDestination setDelimiter(final String delimiter) {
      this.delimiter = delimiter;
      return this;
   }

   /**
    * Gets the current append strategy used to write results to the CSV file.
    *
    * @return The currently used append strategy
    */
   public AppendStrategy getAppendStrategy() {
      return appendStrategy;
   }

   /**
    * Sets the append strategy to be used when writing to the CSV file.
    *
    * @param appendStrategy
    *           The appendStrategy value to set.
    */
   public CsvDestination setAppendStrategy(AppendStrategy appendStrategy) {
      this.appendStrategy = appendStrategy;
      return this;
   }

   /**
    * Gets the data line prefix.
    * @return The data line prefix.
    */
   public String getLinePrefix() {
      return linePrefix;
   }

   /**
    * Sets the data line prefix.
    * This string is written to the output file at the beginning of each line containing data (i.e. not to headers line).
    * @param linePrefix
    *           The data lines prefix.
    */
   public CsvDestination setLinePrefix(String linePrefix) {
      this.linePrefix = linePrefix;
      return this;
   }

   /**
    * Gets the data line suffix.
    * @return The data line suffix.
    */
   public String getLineSuffix() {
      return lineSuffix;
   }

   /**
    * Sets the data line suffix.
    * This string is written to the output file at the end of each line containing data (i.e. not to headers line).
    * @param lineSuffix
    *           The data lines suffix.
    */
   public CsvDestination setLineSuffix(String lineSuffix) {
      this.lineSuffix = lineSuffix;
      return this;
   }

   /**
    * Gets the delimiter used to separate individual lines in the output files.
    * @return The delimiter used to separate output lines.
    */
   public String getLineBreak() {
      return lineBreak;
   }

   /**
    * Sets the delimiter used to separate individual lines in the output files.
    * @param lineBreak
    *           The delimiter used to separate output lines.
    *
    */
   public CsvDestination setLineBreak(String lineBreak) {
      this.lineBreak = lineBreak;
      return this;
   }

   /**
    * When true, headers are not written to the output file.
    * @return True when headers should be written to the output file, false otherwise.
    */
   public boolean isSkipHeader() {
      return skipHeader;
   }

   /**
    * Specifies whether headers should be ommited from the output file.
    * @param skipHeader
    *           When set to true, headers are not written.
    */
   public CsvDestination setSkipHeader(boolean skipHeader) {
      this.skipHeader = skipHeader;
      return this;
   }
}
