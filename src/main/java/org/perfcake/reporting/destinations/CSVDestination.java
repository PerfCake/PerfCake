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
   private String path = "";

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

   @Override
   public void open() {
      csvFile = new File(path);
      if (log.isDebugEnabled()) {
         log.debug("Output path: " + csvFile.getPath());
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
         // make sure the order of columns is consistent
         final boolean csvFileExists = csvFile.exists();
         try (FileOutputStream fos = new FileOutputStream(csvFile, true);
               OutputStreamWriter osw = new OutputStreamWriter(fos, Utils.getDefaultEncoding());
               BufferedWriter bw = new BufferedWriter(osw)) {
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
    *       The path value to set.
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
    *       The delimiter value to set.
    */
   public void setDelimiter(final String delimiter) {
      this.delimiter = delimiter;
   }

}
