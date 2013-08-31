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

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

/**
 * Accumulates average over a set number of recently reported values.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class SlidingWindowAvgAccumulator implements Accumulator<Double> {

   private final Buffer fifo;

   /**
    * Creates a new average accumulator with the sliding window of a given size.
    * 
    * @param windowSize
    *           Size of the sliding window
    */
   public SlidingWindowAvgAccumulator(final int windowSize) {
      fifo = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(windowSize));
   }

   @SuppressWarnings("unchecked")
   @Override
   public void add(final Double value) {
      fifo.add(value);
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

      return size == 0 ? 0d : accum / size;
   }

   @Override
   public void reset() {
      fifo.clear();
   }

}
