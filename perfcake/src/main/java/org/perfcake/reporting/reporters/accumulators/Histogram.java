package org.perfcake.reporting.reporters.accumulators;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by pmacik on 8.10.15.
 */
public class Histogram {

   private Map<Range, Long> histogram = new LinkedHashMap<>();
   private List<Range> ranges = new LinkedList<>();
   private AtomicLong count = new AtomicLong(0);

   public Histogram(final List<Double> rangeDividers) {
      // TODO: Verify, that all the dividers are > minValue and < maxValue
      Collections.sort(rangeDividers);
      final int count = rangeDividers.size();
      double min, max;
      for (int i = 0; i < count; i++) {
         if (i == 0) {
            histogram.put(new Range(Double.NEGATIVE_INFINITY, rangeDividers.get(i)), 0L);
         }
         min = rangeDividers.get(i);

         if (i < count - 1) {
            max = rangeDividers.get(i + 1);
         } else {
            max = Double.POSITIVE_INFINITY;
         }
         histogram.put(new Range(min, max), 0L);
      }

      ranges = new LinkedList<>(histogram.keySet());
      Collections.sort(ranges);
   }

   public void add(final double value) {
      for (Range range : ranges) {
         if (range.contains(value)) {
            histogram.put(range, histogram.get(range) + 1);
            count.incrementAndGet();
            break;
         }
      }
   }

   public void add(final int value) {
      this.add((double) value);
   }

   public Map<Range, Long> getHistogram() {
      return histogram;
   }

   public Long getCount() {
      return count.longValue();
   }
}
