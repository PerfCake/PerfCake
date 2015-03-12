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
 * Accumulates an harmonic mean.
 * Atomic types are not used because both values must be set at the same time. Hence the methods are synchronized.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class HarmonicMeanAccumulator extends SumAccumulator {

   /**
    * Number of reported values.
    */
   private long count = 0;

   @Override
   public synchronized void add(final Double number) {
      super.add(1.0 / number);
      count = count + 1;
   }

   @Override
   public synchronized Double getResult() {
      if (count == 0) {
         return 0d;
      } else {
         return count / super.getResult();
      }
   }

   @Override
   public synchronized void reset() {
      super.reset();
      count = 0;
   }
}
