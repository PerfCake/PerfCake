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
import org.perfcake.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import io.vertx.core.json.JsonArray;

/**
 * Data of a C3 chart stored in the .js file as a script building an array.
 * Does not work with the data header and does not know anything about the actual data.
 * It is the role of {@link C3Chart} to carry all the meta-data.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class C3ChartData {

   /**
    * The individual lines of data.
    */
   private List<JsonArray> data;

   /**
    * Target path where the charts report is stored. The individual data files are located in ${target}/data/${baseName}.js.
    */
   private Path target;

   /**
    * Initialize the class with the provided data. No other actions are taken.
    *
    * @param target
    *       The target path where the chart report is stored. The individual data files are located in ${target}/data/${baseName}.js.
    * @param data
    *       An ordered list of individual data lines.
    */
   private C3ChartData(final Path target, final List<JsonArray> data) {
      this.data = data;
      this.target = target;
   }

   /**
    * Reads the chart data from the appropriate data file. The data file is located in ${target}/data/${baseName}.js.
    *
    * @param baseName
    *       The base of the chart data files name.
    * @param target
    *       The target path where the chart report is stored. The individual data files are located in ${target}/data/${baseName}.js.
    * @throws PerfCakeException
    *       When there was an error reading the data. Mainly because of some I/O error.
    */
   public C3ChartData(final String baseName, final Path target) throws PerfCakeException {
      this(target, new LinkedList<>());

      try {
         List<String> lines = Utils.readLines(Paths.get(target.toString(), "data", baseName + ".js").toUri().toURL());

         for (final String line : lines) {
            if (line.startsWith(baseName)) {
               String jsonArray = line.substring((baseName + ".push(").length());
               jsonArray = jsonArray.substring(0, jsonArray.length() - 2);
               data.add(new JsonArray(jsonArray));
            }
         }

      } catch (IOException e) {
         throw new PerfCakeException("Cannot read chart data: ", e);
      }
   }

   /**
    * Creates a new chart where with only two columns from the original chart. These columns have indexes 0 and the second is specified in the parameter.
    * This is used to create a subchart with the values of the X axis and one data line.
    *
    * @param keepColumnIndex
    *       The index of the column to be kept.
    * @return A new chart where with only two columns - the X axis values and the data line specified in the parameter.
    */
   public C3ChartData filter(int keepColumnIndex) {
      final List<JsonArray> newData = new LinkedList<>();

      data.forEach(array -> {
         List raw = array.getList();
         newData.add(new JsonArray(Arrays.asList(raw.get(0), raw.get(keepColumnIndex))));
      });

      return new C3ChartData(target, newData);
   }

   /**
    * Gets the first line index where there are other values than null (except for the first index column).
    *
    * @return The first line index where there are other values than null
    */
   private int getDataStart() {
      int idx = 0;

      if (data.size() == 0) {
         return -1;
      }

      boolean dataHit;
      do {
         dataHit = false;
         JsonArray a = data.get(idx);

         dataHit = dataHit || !isAllNull(a);

         if (!dataHit) {
            idx++;
         }
      } while (idx < data.size() && !dataHit); // @checkstyle.ignore(RightCurly) - Do-while cycle should have while after the brace.

      return idx;
   }

   /**
    * Checks whether all values in the field are null (except for the first index column).
    *
    * @param a
    *       The array to be checked.
    * @return True if and only if all values in the field are null.
    */
   private static boolean isAllNull(final JsonArray a) {
      int i = 1;
      while (i < a.size()) {
         if (a.getValue(i) != null) {
            return false;
         }
         i++;
      }

      return true;
   }

   /**
    * Gets a list with null values of the given size.
    *
    * @param size
    *       The size of the new list.
    * @return The list with null values of the given size.
    */
   @SuppressWarnings("unchecked")
   private static List getNullList(final int size) {
      final List nullList = new LinkedList<>();
      for (int i = 0; i < size; i++) {
         nullList.add(null);
      }

      return nullList;
   }

   /**
    * Mixes two charts together sorted according to the first index column. Missing data for any index values in either chart are replaced with null.
    * Records with only null values are skipped. The existing chart data are not changed, a new instance is created.
    *
    * @param otherData
    *       The other chart data to be mixed with this chart data.
    * @return A new chart data combining both input charts.
    */
   @SuppressWarnings("unchecked")
   C3ChartData combineWith(final C3ChartData otherData) {
      final List<JsonArray> newData = new LinkedList<>();
      int idx1 = getDataStart();
      int idx2 = otherData.getDataStart();

      if (idx1 == -1) {
         if (idx2 == -1) {
            return new C3ChartData(target, newData);
         } else {
            return new C3ChartData(target, new LinkedList<>(otherData.data));
         }
      } else if (idx2 == -2) {
         return new C3ChartData(target, new LinkedList<>(data));
      }

      int size1 = data.get(0).size();
      List nullList1 = getNullList(size1 - 1);

      int size2 = otherData.data.get(0).size();
      List nullList2 = getNullList(size2 - 1);

      while (idx1 < data.size() || idx2 < otherData.data.size()) {
         JsonArray a1 = idx1 < data.size() ? data.get(idx1) : null;
         JsonArray a2 = idx2 < otherData.data.size() ? otherData.data.get(idx2) : null;
         long p1 = a1 != null ? a1.getLong(0) : Long.MAX_VALUE;
         long p2 = a2 != null ? a2.getLong(0) : Long.MAX_VALUE;
         List raw = new LinkedList<>();

         if (p1 == p2) {
            if (!isAllNull(a1) || !isAllNull(a2)) {
               raw.add(p1);
               raw.addAll(a1.getList().subList(1, size1));
               raw.addAll(a2.getList().subList(1, size2));
            }
            idx1++;
            idx2++;
         } else if (p1 < p2) {
            if (!isAllNull(a1)) {
               raw.add(p1);
               raw.addAll(a1.getList().subList(1, size1));
               raw.addAll(nullList2);
            }
            idx1++;
         } else {
            if (!isAllNull(a2)) {
               raw.add(p2);
               raw.addAll(nullList1);
               raw.addAll(a2.getList().subList(1, size2));
            }
            idx2++;
         }

         if (raw.size() > 0) {
            newData.add(new JsonArray(raw));
         }
      }

      return new C3ChartData(target, newData);
   }

   /**
    * Gets the data lines.
    *
    * @return The data lines.
    */
   public List<JsonArray> getData() {
      return data;
   }

   @Override
   public String toString() {
      return "C3ChartData{" + "data=" + data + '}';
   }
}
