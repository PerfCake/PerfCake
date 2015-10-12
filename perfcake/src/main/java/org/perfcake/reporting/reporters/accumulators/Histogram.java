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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Histogram to count number of representatives for individual ranges.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Histogram {

   private Map<Range, AtomicLong> histogram = new LinkedHashMap<>();
   private List<Range> ranges = new LinkedList<>();
   private AtomicLong count = new AtomicLong(0);

   public Histogram(final List<Double> rangeDividers) {
      // TODO: Verify, that all the dividers are > minValue and < maxValue
      Collections.sort(rangeDividers);
      final int count = rangeDividers.size();
      double min, max;
      for (int i = 0; i < count; i++) {
         if (i == 0) {
            histogram.put(new Range(Double.NEGATIVE_INFINITY, rangeDividers.get(i)), new AtomicLong(0L));
         }
         min = rangeDividers.get(i);

         if (i < count - 1) {
            max = rangeDividers.get(i + 1);
         } else {
            max = Double.POSITIVE_INFINITY;
         }
         histogram.put(new Range(min, max), new AtomicLong(0L));
      }

      ranges = new LinkedList<>(histogram.keySet());
      Collections.sort(ranges);
   }

   public void add(final double value) {
      for (Range range : ranges) {
         if (range.contains(value)) {
            histogram.get(range).incrementAndGet();
            count.incrementAndGet();
            break;
         }
      }
   }

   public void add(final int value) {
      this.add((double) value);
   }

   public Map<Range, Long> getHistogram() {
      Map<Range, Long> result = new HashMap<>();
      Map<Range, AtomicLong> snapshot = new HashMap<>(histogram);

      snapshot.forEach((range, value) -> result.put(range, value.get()));

      return result;
   }

   public Long getCount() {
      return count.longValue();
   }
}
