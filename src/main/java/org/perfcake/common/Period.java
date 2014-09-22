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

import java.io.Serializable;

/**
 * Period represent a certain amount of units of a given type. These units are represented
 * using {@link PeriodType}, i.e. time, iterations, percents.
 * Period is an immutable type.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class Period implements Serializable {

   private static final long serialVersionUID = 3195215553430520740L;

   /**
    * Unit of the period
    */
   private final PeriodType periodType;

   /**
    * Length of the period
    */
   private final long period;

   /**
    * Create a new Period
    *
    * @param periodType
    *       Type of the period
    * @param period
    *       Length of the period
    */
   public Period(final PeriodType periodType, final long period) {
      this.periodType = periodType;
      this.period = period;
   }

   /**
    * Gets current period type.
    *
    * @return period type
    */
   public PeriodType getPeriodType() {
      return periodType;
   }

   /**
    * Get current period length.
    *
    * @return period length
    */
   public long getPeriod() {
      return period;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (period ^ (period >>> 32));
      result = prime * result + ((periodType == null) ? 0 : periodType.hashCode());
      return result;
   }

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
      final Period other = (Period) obj;
      if (period != other.period) {
         return false;
      }
      if (periodType != other.periodType) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return "Period [periodType=" + periodType + ", period=" + period + "]";
   }

}
