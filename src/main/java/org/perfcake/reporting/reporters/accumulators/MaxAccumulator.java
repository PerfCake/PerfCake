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
 * Accumulates the maximum of double values.
 * Atomic types are not used because both values must be set at the same time. Hence the methods are synchronized.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class MaxAccumulator implements Accumulator<Double> {
   /**
    * Maximum of the reported values.
    */
   private Double max = Double.NEGATIVE_INFINITY;

   @Override
   public synchronized void add(final Double value) {
      if (value > max) {
         max = value;
      }
   }

   @Override
   public synchronized Double getResult() {
      return max;
   }

   @Override
   public synchronized void reset() {
      max = Double.NEGATIVE_INFINITY;
   }
}
