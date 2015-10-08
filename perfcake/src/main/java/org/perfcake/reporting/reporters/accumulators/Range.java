package org.perfcake.reporting.reporters.accumulators;

/**
 * Created by pmacik on 8.10.15.
 */
public class Range<T extends Comparable> implements Comparable {
   private T min;
   private T max;

   public Range(final T min, final T max) {
      // TODO: verify min < max;
      if (min.compareTo(max) < 0) {
         this.min = min;
         this.max = max;
      } else {
         throw new IllegalArgumentException("Invalid range boundaries: First argument (" + min + ") should be less then the second (" + max + ").");
      }
   }

   public T getMin() {
      return min;
   }

   public T getMax() {
      return max;
   }

   public boolean contains(final Comparable value) {
      return (value.compareTo(min) >= 0 && value.compareTo(max) <= 0);
   }

   @Override
   public int compareTo(final Object o) {
      Range i = (Range) o;
      if (this.equals(i)) {
         return 0;
      } else if (max.compareTo(i.min) <= 0) {
         return -1;
      } else {
         return 1;
      }
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Range)) {
         return false;
      }

      final Range<?> interval = (Range<?>) o;

      if (getMin() != null ? !getMin().equals(interval.getMin()) : interval.getMin() != null) {
         return false;
      }
      return !(getMax() != null ? !getMax().equals(interval.getMax()) : interval.getMax() != null);

   }

   @Override
   public int hashCode() {
      int result = getMin() != null ? getMin().hashCode() : 0;
      result = 31 * result + (getMax() != null ? getMax().hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "<" + min + "; " + max + '>';
   }
}

