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

package org.perfcake.reporting.destinations.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.perfcake.reporting.ReportsException;

/**
 * Comma separated value file. This file contains values separated by commas :-)
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class CsvFile {
   private static final Logger log = Logger.getLogger(CsvFile.class);

   /**
    * Character to separate things on line
    */
   private static char COMMA = ';';

   private File file;

   public CsvFile(String path) {
      if (path == null || path.equals("")) {
         throw new IllegalArgumentException("Cannot create csv file from null path!");
      }

      file = new File(path);
   }

   /**
    * Creates/overwrites file so that there is clean, empty file.
    */
   public void createNewFile() {
      try {
         if (file.exists()) {
            ;
         }
         file.delete();

         if (!file.createNewFile()) {
            throw new ReportsException("Couldn't create/delete/rewrite Csv file: " + file.getAbsolutePath());
         }
      } catch (IOException e) {
         throw new ReportsException("Couldn't create/delete/rewrite Csv file: " + file.getAbsolutePath(), e);
      }
   }

   /**
    * All lines that are presented in the file.
    */
   public List<String> getLines() {
      BufferedReader br = null;
      List<String> result = new ArrayList<String>();
      if (!file.exists()) {
         throw new RuntimeException("File [" + file.getAbsolutePath() + "] cannot be read because it doesn't exist!");
      }

      try {
         br = new BufferedReader(new FileReader(file));

         String line;
         while ((line = br.readLine()) != null) {
            result.add(line);
         }

         return result;
      } catch (Exception ex) {
         throw new ReportsException("Couldn't read from Csv file: " + file.getAbsolutePath(), ex);
      } finally {
         try {
            if (br != null) {
               br.close();
            }
         } catch (IOException e) {
            log.error("Error while closing CSV file after reading in finally", e);
         }
      }
   }

   public String getAllText() {
      List<String> lines = getLines();

      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (String line : lines) {
         if (!first) {
            sb.append("\n");
         }

         first = false;
         sb.append(line);
      }
      return sb.toString();
   }

   /**
    * Appends line of COMMA separated items that are supplied as parameters and
    * appends newLine character.
    * 
    * @param line
    */
   public void appendLine(String... line) {
      BufferedWriter bw = null;
      try {
         bw = new BufferedWriter(new FileWriter(file, true));

         // First is hack because java doesn't have String.join
         boolean first = true;
         for (String item : line) {
            if (!first) {
               bw.write(COMMA);
            }
            first = false;

            bw.write(item == null ? "" : item.trim());
         }
         bw.newLine();

      } catch (IOException e) {
         throw new ReportsException("Couldn't append line into Csv file: " + file.getAbsolutePath(), e);
      } finally {
         try {
            if (bw != null) {
               bw.close();
            }
         } catch (IOException e) {
            log.error("Error while closing CSV file in finally", e);
         }
      }
   }
}
