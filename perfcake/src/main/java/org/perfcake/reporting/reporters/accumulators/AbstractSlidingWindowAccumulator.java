/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2014 the original author or authors.
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
 * Accumulates a value over a set of recently reported values in a sliding window.
 * The sliding window is a number of last values that are accumulated.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public abstract class AbstractSlidingWindowAccumulator implements Accumulator<Double> {

   protected final Buffer fifo;

   /**
    * Creates a new accumulator with the sliding window of a given size.
    *
    * @param windowSize
    *       Size of the sliding window.
    */
   public AbstractSlidingWindowAccumulator(final int windowSize) {
      fifo = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(windowSize));
   }

   @SuppressWarnings("unchecked")
   @Override
   public void add(final Double value) {
      fifo.add(value);
   }

   @Override
   public void reset() {
      fifo.clear();
   }
}
