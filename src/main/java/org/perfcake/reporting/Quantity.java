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
package org.perfcake.reporting;

/**
 * A number with an unit.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Quantity<N extends Number> {
   private final N number;
   private final String unit;

   /**
    * Creates a new quantity.
    *
    * @param number
    *       The value.
    * @param unit
    *       The unit of the value.
    */
   public Quantity(final N number, final String unit) {
      this.number = number;
      this.unit = unit;
   }

   /**
    * Gets the current value.
    *
    * @return The current value.
    */
   public N getNumber() {
      return number;
   }

   /**
    * Gets the unit of the value.
    *
    * @return The unit of the value.
    */
   public String getUnit() {
      return unit;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((number == null) ? 0 : number.hashCode());
      result = prime * result + ((unit == null) ? 0 : unit.hashCode());
      return result;
   }

   @SuppressWarnings("rawtypes")
   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      final Quantity other = (Quantity) obj;
      if (number == null) {
         if (other.number != null) {
            return false;
         }
      } else if (!number.equals(other.number)) {
         return false;
      }
      if (unit == null) {
         if (other.unit != null) {
            return false;
         }
      } else if (!unit.equals(other.unit)) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return number + " " + unit;
   }
}
