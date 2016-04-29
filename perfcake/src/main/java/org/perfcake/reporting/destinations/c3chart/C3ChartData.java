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
import org.perfcake.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import io.vertx.core.json.JsonArray;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class C3ChartData {

   private List<JsonArray> data;

   private C3ChartData(final List<JsonArray> data) {
      this.data = data;
   }

   public C3ChartData(final String baseName, final Path target) throws PerfCakeException {
      this(new LinkedList<>());

      try {
         List<String> lines = Utils.readLines(Paths.get(target.toString(), "data", baseName + ".js").toUri().toURL());

         for (final String line: lines) {
            if (line.startsWith(baseName)) {
               String jsonArray = line.substring((baseName + ".push(").length());
               jsonArray = jsonArray.substring(0, jsonArray.length() - 2);
               data.add(new JsonArray(jsonArray));
            }
         }
         System.out.println(data);
         System.out.println(Arrays.asList(null, null, null));

      } catch (IOException e) {
         throw new PerfCakeException("Cannot read chart data: ", e);
      }
   }

   public C3ChartData filter(int keepColumnIndex) {
      final List<JsonArray> newData = new LinkedList<>();

      data.forEach(array -> {
         List raw = array.getList();
         newData.add(new JsonArray(Arrays.asList(raw.get(0), raw.get(keepColumnIndex))));
      });

      return new C3ChartData(newData);
   }

   @SuppressWarnings("unchecked")
   public C3ChartData combineWith(final C3ChartData otherData) {
      final List<JsonArray> newData = new LinkedList<>();
      int idx1 = 0, idx2 = 0;

      int size1 = data.get(0).size();
      List nullList1 = new LinkedList<>();
      for (int i = 0; i < size1 - 1; i++) {
         nullList1.add(null);
      }

      int size2 = otherData.data.get(0).size();
      List nullList2 = new LinkedList<>();
      for (int i = 0; i < size2 - 1; i++) {
         nullList2.add(null);
      }

      while (idx1 < data.size() || idx2 < otherData.data.size()) {
         JsonArray a1 = data.get(idx1);
         JsonArray a2 = otherData.data.get(idx2);
         long p1 = a1.getLong(0);
         long p2 = a2.getLong(0);
         List raw = new LinkedList<>();

         if (p1 == p2) {
            raw.add(p1);
            raw.addAll(a1.getList().subList(1, size1));
            raw.addAll(a2.getList().subList(1, size2));
            idx1++;
            idx2++;
         } else if (p1 < p2) {
            raw.add(p1);
            raw.addAll(a1.getList().subList(1, size1));
            raw.addAll(nullList2);
            idx1++;
         } else {
            raw.add(p2);
            raw.addAll(nullList1);
            raw.addAll(a2.getList().subList(1, size2));
            idx2++;
         }

         newData.add(new JsonArray(raw));
      }

      return new C3ChartData(newData);
   }

   @Override
   public String toString() {
      return "C3ChartData{" +
            "data=" + data +
            '}';
   }
}
