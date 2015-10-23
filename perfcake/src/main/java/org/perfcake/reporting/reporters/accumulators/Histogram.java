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
package org.perfcake.reporting.reporters.accumulators;

import org.perfcake.util.StringUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Histogram to count number of representatives for individual ranges.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Histogram {

   /**
    * Actual counts of occurrences for given ranges,
    */
   private Map<Range, AtomicLong> histogram = new LinkedHashMap<>();

   /**
    * Ranges of the histogram.
    */
   private List<Range> ranges = new LinkedList<>();

   /**
    * Creates the histogram based on the comma separated string of range split points.
    *
    * @param rangeDividers
    *       The comma separated string of range split points.
    */
   public Histogram(String rangeDividers) {
      this(Arrays.asList(rangeDividers.split(",")).stream().map(StringUtil::trim).map(Double::valueOf).sorted().collect(Collectors.toList()));
   }

   /**
    * Creates a new histogram that is composed of the ranges divided at the given points.
    *
    * @param rangeDividers
    *       Points specifying where the histogram should be split to multiple ranges.
    */
   public Histogram(final List<Double> rangeDividers) {
      Collections.sort(rangeDividers);
      final int count = rangeDividers.size();
      double min, max;
      for (int i = 0; i < count; i++) {
         min = rangeDividers.get(i);

         if (i == 0) {
            histogram.put(new Range(Double.NEGATIVE_INFINITY, min), new AtomicLong(0L));
         }

         if (i < count - 1) {
            max = rangeDividers.get(i + 1);
         } else {
            max = Double.POSITIVE_INFINITY;
         }

         if (min < max) {
            histogram.put(new Range(min, max), new AtomicLong(0L));
         }
      }

      ranges = new LinkedList<>(histogram.keySet());
      Collections.sort(ranges);
   }

   /**
    * Adds a new value to be counted in the histogram.
    *
    * @param value
    *       The new value to be added and counted.
    */
   public void add(final double value) {
      for (Range range : ranges) {
         if (range.contains(value)) {
            histogram.get(range).incrementAndGet();
            break;
         }
      }
   }

   /**
    * Adds a new value to be counted in the histogram.
    *
    * @param value
    *       The new value to be added and counted.
    */
   public void add(final int value) {
      this.add((double) value);
   }

   /**
    * Gets the actual counts for individual ranges of the histogram.
    *
    * @return The actual counts for individual ranges of the histogram.
    */
   @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "UC_USELESS_OBJECT", justification = "We copied the histogram hash map to get a frozen state.")
   public Map<Range, Long> getHistogram() {
      Map<Range, Long> result = new LinkedHashMap<>();
      Map<Range, AtomicLong> snapshot = new LinkedHashMap<>(histogram);

      snapshot.forEach((range, value) -> result.put(range, value.get()));

      return result;
   }

   /**
    * Gets the actual counts for individual ranges of the histogram.
    *
    * @return The actual counts for individual ranges of the histogram.
    */
   @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "UC_USELESS_OBJECT", justification = "We copied the histogram hash map to get a frozen state.")
   public Map<Range, Double> getHistogramInPercent() {
      Map<Range, Double> result = new LinkedHashMap<>();
      Map<Range, AtomicLong> snapshot = new LinkedHashMap<>(histogram);
      final long count = getCount(snapshot);

      if (count == 0) {
         snapshot.forEach((range, value) -> result.put(range, 0d));
      } else {
         snapshot.forEach((range, value) -> result.put(range, ((double) value.get()) / count * 100.0d));
      }

      return result;
   }

   /**
    * Gets the total number of values counted in the histogram.
    *
    * @return The total number of values counted in the histogram.
    */
   public Long getCount() {
      return getCount(new LinkedHashMap<>(histogram));
   }

   /** Gets the sum of all map entries for the given map snapshot.
    *
    * @param snapshot The map snapshot to be counted.
    * @return The sum of all map entries.
    */
   private static long getCount(Map<Range, AtomicLong> snapshot) {
      long count = 0;
      for (AtomicLong l : snapshot.values()) {
         count = count + l.get();
      }

      return count;
   }
}
