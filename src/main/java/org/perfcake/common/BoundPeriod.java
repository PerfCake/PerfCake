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
package org.perfcake.common;

/**
 * A {@link Period} bound to a specific object. The binding means that the
 * BoundPeriod is valid only on the given object and an action should be taken only for this object.
 *
 * @param <T>
 *       Class of the bound object.
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class BoundPeriod<T> extends Period {

   private static final long serialVersionUID = 8290675980551305133L;

   /**
    * The object bound to this period.
    */
   private final T binding;

   /**
    * Creates a new BoundPeriod.
    *
    * @param periodType
    *       Period unit type.
    * @param period
    *       Period length.
    * @param binding
    *       Object bound to this period.
    */
   public BoundPeriod(final PeriodType periodType, final long period, final T binding) {
      super(periodType, period);
      this.binding = binding;
   }

   /**
    * Creates a new BoundPeriod based on an existing Period.
    *
    * @param p
    *       Existing period.
    * @param binding
    *       Object bound to this period.
    */
   public BoundPeriod(final Period p, final T binding) {
      this(p.getPeriodType(), p.getPeriod(), binding);
   }

   /**
    * Gets the object bound to this period.
    *
    * @return The object bound to this period.
    */
   public T getBinding() {
      return binding;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((binding == null) ? 0 : binding.hashCode());
      return result;
   }

   @SuppressWarnings("rawtypes")
   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (!super.equals(obj)) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      final BoundPeriod other = (BoundPeriod) obj;
      if (binding == null) {
         if (other.binding != null) {
            return false;
         }
      } else if (!binding.equals(other.binding)) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return "BoundPeriod [binding=" + binding + ", periodType=" + getPeriodType() + ", period=" + getPeriod() + "]";
   }
}
