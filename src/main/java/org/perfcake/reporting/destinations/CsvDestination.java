/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.reporting.destinations;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportsException;
import org.perfcake.reporting.destinations.util.CsvFile;

/**
 * <p>
 * This destination can be set following properties:
 * <table border="1">
 * <tr>
 * <td>Property name</td>
 * <td>Description</td>
 * <td>Required</td>
 * <td>Sample value</td>
 * <td>Default</td>
 * </tr>
 * <tr>
 * <td>outputPath</td>
 * <td>Directory into which the reported CSV should be put</td>
 * <td>YES</td>
 * <td>\tmp\output</td>
 * <td></td>
 * </tr>
 * </table>
 */
public class CsvDestination extends Destination {

   private String outputPath = null;

   private static final Logger log = Logger.getLogger(CsvDestination.class);

   private Map<String, CsvFile> createdCsvs = new HashMap<String, CsvFile>();

   private Object sendLock = new Object();

   @Override
   public void loadSpecificConfigValues() throws ReportsException {
      if (!new File(outputPath).exists()) {
         log.warn("The output folder for CSV destination [" + outputPath + "] doesn't exist. Trying to create one.");
         boolean success = new File(outputPath).mkdirs();
         if (!success) {
            throw new ReportsException("The output folder for CSV destination [" + outputPath + "] doesn't exist and couldn't be created!");
         }
      }
   }

   @Override
   public void send() throws ReportsException {
      synchronized (sendLock) {
         while (!messageQueue.isEmpty()) {
            Measurement m = messageQueue.peek();
            ensureCsvExists(m);
            outputToCsv(m);
            messageQueue.poll();
         }
      }
   }

   /**
    * Outputs one file Measurement into CSV, this method expects that the csv
    * exists.
    * 
    * @throws ReportsException
    * 
    */
   private void outputToCsv(Measurement m) throws ReportsException {
      CsvFile csv = createdCsvs.get(m.getMeasurementType() + m.getLabelType());
      csv.appendLine(m.getLabel(), m.getValue());
   }

   /**
    * Ensures that csv for this measurement exists. 1 CSV file for each
    * combination of measurementType and labelType should be created.
    * 
    * @param m
    * @throws ReportsException
    */
   private void ensureCsvExists(Measurement m) throws ReportsException {
      if (createdCsvs.containsKey(m.getMeasurementType() + m.getLabelType())) {
         return;
      }
      String filename = outputPath + System.getProperty("file.separator") + testRunInfo.getUniqueId() + "_" + m.getMeasurementType() + "_" + m.getLabelType() + ".csv";
      CsvFile csv = new CsvFile(filename);
      csv.createNewFile();
      csv.appendLine(m.getLabelType(), m.getMeasurementType());
      createdCsvs.put(m.getMeasurementType() + m.getLabelType(), csv);

   }

   public String getOutputPath() {
      return outputPath;
   }

   public void setOutputPath(String outputPath) {
      this.outputPath = outputPath;
   }

}
