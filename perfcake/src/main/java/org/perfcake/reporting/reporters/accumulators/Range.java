package org.perfcake.reporting.reporters.accumulators;

/**
 * Created by pmacik on 8.10.15.
 */
public class Range implements Comparable<Range> {
   private double min;
   private double max;

   public Range(final double min, final double max) {
      if (min < max) {
         this.min = min;
         this.max = max;
      } else {
         throw new IllegalArgumentException("Invalid range boundaries: First argument (" + min + ") should be less then the second (" + max + ").");
      }
   }

   public Range(final int min, final int max) {
      this((double) min, (double) max);
   }

   public double getMin() {
      return min;
   }

   public double getMax() {
      return max;
   }

   public boolean contains(final double value) {
      return (value >= min && value <= max);
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Range)) {
         return false;
      }

      final Range range = (Range) o;

      if (Double.compare(range.getMin(), getMin()) != 0) {
         return false;
      }
      return Double.compare(range.getMax(), getMax()) == 0;

   }

   @Override
   public int hashCode() {
      int result;
      long temp;
      temp = Double.doubleToLongBits(getMin());
      result = (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(getMax());
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
   }

   @Override
   public int compareTo(final Range o) {
      if (this.equals(o)) {
         return 0;
      } else if (this.max <= o.min) {
         return -1;
      } else {
         return 1;
      }
   }

   @Override
   public String toString() {
      return "<" + min + "; " + max + '>';
   }
}

