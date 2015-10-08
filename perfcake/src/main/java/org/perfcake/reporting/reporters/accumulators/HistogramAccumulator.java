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
public class HistogramAccumulator implements Accumulator<Object> {

   private Map<Range<? extends Comparable>, Long> histogram = new LinkedHashMap<>();
   private List<Range<? extends Comparable>> ranges = new LinkedList<>();
   private Comparable minValue;
   private Comparable maxValue;
   private AtomicLong count = new AtomicLong(0);

   public HistogramAccumulator(final List<? extends Comparable> rangeDividers, final Comparable minValue, final Comparable maxValue) {
      this.minValue = minValue;
      this.maxValue = maxValue;
      // TODO: Verify, that all the dividers are > minValue and < maxValue
      Collections.sort(rangeDividers);
      final int count = rangeDividers.size();
      Comparable min, max;
      for (int i = 0; i < count; i++) {
         if (i == 0) {
            histogram.put(new Range(minValue, rangeDividers.get(i)), 0L);
         }
         min = rangeDividers.get(i);

         if (i < count - 1) {
            max = rangeDividers.get(i + 1);
         } else {
            max = maxValue;
         }
         histogram.put(new Range(min, max), 0L);
      }

      ranges = new LinkedList<>(histogram.keySet());
      Collections.sort(ranges);
   }

   @Override
   public void add(final Object value) {
      for (Range range : ranges) {
         if (range.contains((Comparable) value)) {
            histogram.put(range, histogram.get(range) + 1);
            count.incrementAndGet();
            break;
         }
      }
   }

   @Override
   public Object getResult() {
      return null;
   }

   public Map<Range<? extends Comparable>, Long> getHistogram() {
      return histogram;
   }

   public Long getCount() {
      return count.longValue();
   }

   @Override
   public void reset() {
   }
}
