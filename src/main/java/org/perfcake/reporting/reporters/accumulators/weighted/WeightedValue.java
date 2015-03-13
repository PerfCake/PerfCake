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
package org.perfcake.reporting.reporters.accumulators.weighted;

/**
 * Weighted value is a wrapper for a numeric value with some weight.
 * This is used for statistics. An instance is immutable.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class WeightedValue<T extends Number> {
   private final T value;
   private final long weight;

   /**
    * Creates a new immutable instance with the specified value and weight.
    *
    * @param value
    *       The value to be stored.
    * @param weight
    *       The weight of the value.
    */
   public WeightedValue(final T value, final long weight) {
      this.value = value;
      this.weight = weight;
   }

   /**
    * Gets the stored value.
    *
    * @return The stored value.
    */
   public T getValue() {
      return value;
   }

   /**
    * Gets the weight of the value.
    *
    * @return The weight of the value.
    */
   public long getWeight() {
      return weight;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof WeightedValue)) {
         return false;
      }

      final WeightedValue that = (WeightedValue) o;

      if (weight != that.weight) {
         return false;
      }
      if ((value != null) ? !value.equals(that.value) : (that.value != null)) {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode() {
      int result = value != null ? value.hashCode() : 0;
      result = 31 * result + (int) (weight ^ (weight >>> 32));
      return result;
   }

   @Override
   public String toString() {
      if (weight == 1) {
         return value.toString();
      } else {
         return "[" + value + ", weight=" + weight + ']';
      }
   }
}
