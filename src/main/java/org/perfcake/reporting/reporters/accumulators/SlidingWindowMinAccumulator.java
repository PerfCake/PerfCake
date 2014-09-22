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
 * Accumulates a minimal value over a set number of recently reported values.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class SlidingWindowMinAccumulator extends AbstractSlidingWindowAccumulator {

   public SlidingWindowMinAccumulator(final int windowSize) {
      super(windowSize);
   }

   @Override
   public Double getResult() {
      double min = Double.MAX_VALUE;
      synchronized (fifo) {
         for (Object o : fifo) {
            min = Math.min(min, (Double) o);
         }
      }

      return fifo.size() == 0 ? Double.NaN : min;
   }
}
