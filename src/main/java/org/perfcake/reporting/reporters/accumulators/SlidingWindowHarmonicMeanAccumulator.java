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
 * Accumulates an harmonic mean over a set number of recently reported values.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class SlidingWindowHarmonicMeanAccumulator extends AbstractSlidingWindowAccumulator {

   public SlidingWindowHarmonicMeanAccumulator(int windowSize) {
      super(windowSize);
   }

   @SuppressWarnings("unchecked")
   @Override
   public void add(final Double number) {
      fifo.add(1.0 / number);
   }

   @Override
   public Double getResult() {
      double accum = 0;
      double size = 0;

      synchronized (fifo) {
         for (Object o : fifo) {
            accum = accum + (Double) o;
         }
         size = fifo.size();
      }

      return size == 0 ? 0d : size / accum;
   }
}
