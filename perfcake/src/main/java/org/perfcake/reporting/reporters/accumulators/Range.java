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

/**
 * Represents a mathematical range &lt;a, b).
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Range implements Comparable<Range> {

   /**
    * Left border of the range.
    */
   private double min;

   /**
    * Right border of the interval.
    */
   private double max;

   /**
    * Creates a new range based on the specified borders as &lt;a, b).
    *
    * @param min
    *       Left border of the range.
    * @param max
    *       Right border of the range.
    */
   public Range(final double min, final double max) {
      if (min < max) {
         this.min = min;
         this.max = max;
      } else {
         throw new IllegalArgumentException("Invalid range boundaries: First argument (" + min + ") should be less then the second (" + max + ").");
      }
   }

   /**
    * Creates a new range based on the specified borders as &lt;a, b).
    *
    * @param min
    *       Left border of the range.
    * @param max
    *       Right border of the range.
    */
   public Range(final int min, final int max) {
      this((double) min, (double) max);
   }

   /**
    * Gets the left border of the range.
    *
    * @return The left border of the range.
    */
   public double getMin() {
      return min;
   }

   /**
    * Gets the right border of the range.
    *
    * @return The right border of the range.
    */
   public double getMax() {
      return max;
   }

   /**
    * Finds out whether the value lies in the range.
    *
    * @param value
    *       The value to be checked.
    * @return True iff the value is in the range.
    */
   public boolean contains(final double value) {
      return (value >= min && value < max);
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
      return "<" + min + ":" + max + ')';
   }
}

