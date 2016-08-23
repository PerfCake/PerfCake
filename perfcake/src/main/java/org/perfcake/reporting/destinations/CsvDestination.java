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
package org.perfcake.reporting.destinations;

import org.perfcake.PerfCakeConst;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.util.DataBuffer;
import org.perfcake.util.Utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Appends a {@link org.perfcake.reporting.Measurement} into a CSV file.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CsvDestination extends AbstractDestination {

   /**
    * Logger.
    */
   private static final Logger log = LogManager.getLogger(CsvDestination.class);

   /**
    * Caching the state of trace logging level to speed up reporting.
    */
   private static final boolean logTrace = log.isTraceEnabled();

   /**
    * The list containing names of results from measurement.
    */
   private final List<String> resultNames = new ArrayList<>();

   /**
    * A comma separated list of expected attributes, that should be present in measurement to be published.
    */
   private List<String> expectedAttributes = new ArrayList<>();

   /**
    * Here we cache the result of expectedAttributes.isEmpty().
    */
   private boolean expectedAttributesEmpty = false;

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
    * File channel for storing the resulting CSV file.
    */
   private FileChannel outputChannel;

   /**
    * Strategy that is used in case that the output file, that this destination represents
    * was used by a different destination or scenario run before.
    */
   private AppendStrategy appendStrategy = AppendStrategy.RENAME;

   /**
    * A strategy that determines the destination's behavior for a case that the value of an expected attribute in report to be published is missing.
    */
   private MissingStrategy missingStrategy = MissingStrategy.NULL;

   /**
    * Some attributes might end with an asterisk, in such a case, we are not able to create output until the end of the test.
    */
   private boolean dynamicAttributes = false;

   /**
    * True when the warmUp attribute was required in expectedFields.
    */
   private boolean wasWarmUp = false;

   /**
    * Holds the data when the dynamic attributes are used and we cannot stream directly to a file.
    */
   private DataBuffer buffer;

   @Override
   public void open() {
      dynamicAttributes = expectedAttributes.stream().anyMatch(s -> s.endsWith("*"));
      wasWarmUp = expectedAttributes.contains(PerfCakeConst.WARM_UP_TAG); // this gets removed by dataBuffer, so we'll need to put it back later

      csvFile = new File(path);

      if (dynamicAttributes) {
         buffer = new DataBuffer(expectedAttributes);
      } else {
         openFile();
      }
   }

   @Override
   public void close() {
      if (dynamicAttributes) {
         expectedAttributes = buffer.getAttributes();

         if (wasWarmUp) {
            expectedAttributes.add(PerfCakeConst.WARM_UP_TAG);
         }

         openFile();

         buffer.replay((measurement) -> {
            try {
               realReport(measurement);
            } catch (ReportingException e) {
               log.error("Unable to write all reported data: ", e);
            }
         }, false);
      }

      closeFile();
   }

   @Override
   public void report(final Measurement measurement) throws ReportingException {
      if (dynamicAttributes) {
         buffer.record(measurement);
      } else {
         realReport(measurement);
      }
   }

   /**
    * Opens the result file according to the configured overwrite strategy.
    */
   private void openFile() {
      if (csvFile.exists()) {
         switch (appendStrategy) {
            case RENAME:
               final String name = csvFile.getAbsolutePath();
               File f;
               int ind = 1;
               do {
                  //
                  final int lastDot = name.lastIndexOf(".");
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
            case APPEND:
            default:
               // nothing to do here
         }
      }

      if (log.isDebugEnabled()) {
         log.debug(String.format("Opened CSV destination to the file %s.", path));
      }
   }

   /**
    * Closes the result file.
    */
   private void closeFile() {
      if (outputChannel != null) {
         try {
            outputChannel.close();
         } catch (final IOException e) {
            log.error(String.format("Could not close file channel with CSV results for file %s.", csvFile), e);
         }
      }
      csvFile = null;
   }

   /**
    * Autocompute the expectedAttributes when theyw ere not specified by the user.
    *
    * @param measurement
    *       A sample measurement to read the attributes from.
    */
   private void presetResultNames(final Measurement measurement) {
      expectedAttributesEmpty = expectedAttributes.isEmpty();
      if (expectedAttributesEmpty) {

         final Map<String, Object> results = measurement.getAll();
         resultNames.addAll(results.keySet().stream().filter(key -> !key.equals(Measurement.DEFAULT_RESULT)).collect(Collectors.toList()));
      } else {
         resultNames.addAll(expectedAttributes);
      }
   }

   /**
    * Gets the CSV result file header based on the attributes in the measurement.
    *
    * @param measurement
    *       The measurement to read the attributes from.
    * @return The CSV result file header string.
    */
   private String getFileHeader(final Measurement measurement) {
      final StringBuilder sb = new StringBuilder();
      final Object defaultResult = measurement.get();

      sb.append("Time");
      sb.append(delimiter);
      sb.append("Iterations");
      if (defaultResult != null) {
         sb.append(delimiter);
         sb.append(Measurement.DEFAULT_RESULT);
      }
      for (final String key : resultNames) {
         sb.append(delimiter);
         sb.append(key);
      }

      return sb.toString();
   }

   /**
    * Gets a single CSV result file line based on the measurement.
    *
    * @param measurement
    *       The measurement to be reported in the CSV result file.
    * @return A new line entry to the CSV result file.
    */
   private String getResultsLine(final Measurement measurement) {
      final Object defaultResult = measurement.get();
      final Map<String, Object> results = measurement.getAll();
      final StringBuilder sb = new StringBuilder();

      sb.append(Utils.timeToHms(measurement.getTime()));
      sb.append(delimiter);
      sb.append(measurement.getIteration() + 1);

      if (defaultResult != null) {
         sb.append(delimiter);
         if (defaultResult instanceof Quantity<?>) {
            sb.append(((Quantity<?>) defaultResult).getNumber());
         } else {
            sb.append(defaultResult);
         }
      }

      Object currentResult;
      for (final String resultName : resultNames) {
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

   /**
    * Performs the real reporting of the measurement.
    *
    * @param measurement
    *       The measurement to be reported.
    * @throws ReportingException
    *       If it was not possible the write the reported data to the result file.
    */
   private void realReport(final Measurement measurement) throws ReportingException {
      // make sure the order of columns is consistent
      if (resultNames.isEmpty()) { // performance optimization before we enter the sync. block
         presetResultNames(measurement);
         fileHeaders = getFileHeader(measurement);
      }

      if (!expectedAttributesEmpty) {
         if (MissingStrategy.SKIP.equals(missingStrategy)) {
            final Set<String> measurementResults = measurement.getAll().keySet();
            final List<String> missingAttributes = resultNames.stream().filter(ea -> !measurementResults.contains(ea)).collect(Collectors.toList());
            if (!missingAttributes.isEmpty()) {
               if (logTrace) {
                  log.trace("Expected attributes " + missingAttributes.toString() + " are missing from results " + measurement.getAll().toString() + ". Skipping this entry.");
               }
               return;
            }
         }
      }

      final StringBuilder sb = new StringBuilder();
      if (linePrefix != null && !linePrefix.isEmpty()) {
         sb.append(linePrefix);
      }

      sb.append(getResultsLine(measurement));

      if (lineSuffix != null && !lineSuffix.isEmpty()) {
         sb.append(lineSuffix);
      }
      sb.append(lineBreak);

      try {
         final boolean csvFileExists = csvFile.exists();

         if (outputChannel == null) {
            outputChannel = FileChannel.open(csvFile.toPath(), csvFileExists ? StandardOpenOption.APPEND : StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         }

         if (!csvFileExists && !skipHeader) {
            sb.insert(0, fileHeaders + lineBreak);
         }

         outputChannel.write(ByteBuffer.wrap(sb.toString().getBytes(Charset.forName(Utils.getDefaultEncoding()))));
      } catch (final IOException ioe) {
         throw new ReportingException(String.format("Could not append a report to the file %s.", csvFile.getPath()), ioe);
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
    *       The output file path to be set.
    * @return Instance of this to support fluent API.
    */
   public CsvDestination setPath(final String path) {
      if (csvFile != null) {
         throw new UnsupportedOperationException("Changing the value of path after opening the destination is not allowed.");
      }
      if (outputChannel != null) {
         try {
            outputChannel.close();
            outputChannel = null;
         } catch (final IOException e) {
            log.error(String.format("Could not close file channel with CSV results for file %s.", csvFile), e);
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
    *       The delimiter to be used between data elements in an output line.
    * @return Instance of this to support fluent API.
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
    *       The appendStrategy value to set.
    * @return Instance of this to support fluent API.
    */
   public CsvDestination setAppendStrategy(final AppendStrategy appendStrategy) {
      this.appendStrategy = appendStrategy;
      return this;
   }

   /**
    * Gets the current missing strategy used to write results to the CSV file.
    *
    * @return The currently used missing strategy
    */
   public MissingStrategy getMissingStrategy() {
      return missingStrategy;
   }

   /**
    * Sets the missing strategy to be used when writing to the CSV file.
    *
    * @param missingStrategy
    *       The missingStrategy value to set.
    * @return Instance of this to support fluent API.
    */
   public CsvDestination setMissingStrategy(final MissingStrategy missingStrategy) {
      this.missingStrategy = missingStrategy;
      return this;
   }

   /**
    * Gets the exppected attributes that will be written to the CSV file.
    *
    * @return The exppected attributes separated by comma.
    */
   public String getExpectedAttributes() {
      return StringUtils.join(expectedAttributes, ",");
   }

   /**
    * Sets the exppected attributes that will be written to the CSV file.
    *
    * @param expectedAttributes
    *       The exppected attributes separated by comma.
    * @return Instance of this to support fluent API.
    */
   public CsvDestination setExpectedAttributes(final String expectedAttributes) {
      if (expectedAttributes == null || "".equals(expectedAttributes)) {
         this.expectedAttributes = new ArrayList<>();
      } else {
         this.expectedAttributes = new ArrayList<>(Arrays.asList(expectedAttributes.split("\\s*,\\s*")));
      }

      return this;
   }

   /**
    * Gets the exppected attributes that will be written to the CSV file as a List.
    *
    * @return The attributes list.
    */
   public List<String> getExpectedAttributesAsList() {
      return expectedAttributes;
   }

   /**
    * Gets the data line prefix.
    *
    * @return The data line prefix.
    */
   public String getLinePrefix() {
      return linePrefix;
   }

   /**
    * Sets the data line prefix.
    * This string is written to the output file at the beginning of each line containing data (i.e. not to headers line).
    *
    * @param linePrefix
    *       The data lines prefix.
    * @return Instance of this to support fluent API.
    */
   public CsvDestination setLinePrefix(final String linePrefix) {
      this.linePrefix = linePrefix;
      return this;
   }

   /**
    * Gets the data line suffix.
    *
    * @return The data line suffix.
    */
   public String getLineSuffix() {
      return lineSuffix;
   }

   /**
    * Sets the data line suffix.
    * This string is written to the output file at the end of each line containing data (i.e. not to headers line).
    *
    * @param lineSuffix
    *       The data lines suffix.
    * @return Instance of this to support fluent API.
    */
   public CsvDestination setLineSuffix(final String lineSuffix) {
      this.lineSuffix = lineSuffix;
      return this;
   }

   /**
    * Gets the delimiter used to separate individual lines in the output files.
    *
    * @return The delimiter used to separate output lines.
    */
   public String getLineBreak() {
      return lineBreak;
   }

   /**
    * Sets the delimiter used to separate individual lines in the output files.
    *
    * @param lineBreak
    *       The delimiter used to separate output lines.
    * @return Instance of this to support fluent API.
    */
   public CsvDestination setLineBreak(final String lineBreak) {
      this.lineBreak = lineBreak;
      return this;
   }

   /**
    * When true, headers are not written to the output file.
    *
    * @return True when headers should be written to the output file, false otherwise.
    */
   public boolean isSkipHeader() {
      return skipHeader;
   }

   /**
    * Specifies whether headers should be ommited from the output file.
    *
    * @param skipHeader
    *       When set to true, headers are not written.
    * @return Instance of this to support fluent API.
    */
   public CsvDestination setSkipHeader(final boolean skipHeader) {
      this.skipHeader = skipHeader;
      return this;
   }

   /**
    * Determines the strategy for a case that the output file exists. {@link AppendStrategy#OVERWRITE} means that the file
    * is overwritten, {@link AppendStrategy#RENAME} means that the current output file is renamed by adding a number-based
    * suffix and {@link AppendStrategy#APPEND} is for appending new results to the original file.
    *
    * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
    */
   public enum AppendStrategy {
      /**
       * The original file is overwritten.
       */
      OVERWRITE,

      /**
       * The original file is left alone but the output file is renamed according to a number-based pattern.
       */
      RENAME,

      /**
       * The measurements are appended to the original file.
       */
      APPEND
   }

   /**
    * Determines the strategy for a case that the value of an expected attribute in report to be published is missing.
    *
    * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
    */
   public enum MissingStrategy {
      /**
       * The records with the missing values are skipped/ignored.
       */
      SKIP,

      /**
       * The missing values are replaced by <code>null</code> strings in the output.
       */
      NULL
   }
}
