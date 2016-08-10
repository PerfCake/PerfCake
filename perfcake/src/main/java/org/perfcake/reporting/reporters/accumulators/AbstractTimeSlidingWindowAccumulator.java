/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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

import org.perfcake.common.TimeSlidingWindow;

/**
 * Accumulates a value over a set of recently reported values in a sliding window.
 * The sliding window is a time period in milliseconds.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public abstract class AbstractTimeSlidingWindowAccumulator<T, A extends Accumulator<T>> implements Accumulator<T> {

   protected final TimeSlidingWindow<T> window;
   protected A accum;

   /**
    * Creates a new accumulator with the sliding window of a given time period.
    *
    * @param windowSize
    *       Size of the sliding window.
    */
   public AbstractTimeSlidingWindowAccumulator(final int windowSize, final Class<A> accumClass) {
      window = new TimeSlidingWindow<>(windowSize);
      try {
         this.accum = accumClass.newInstance();
      } catch (InstantiationException e) {
         e.printStackTrace();
      } catch (IllegalAccessException e) {
         e.printStackTrace();
      }
   }

   @Override
   public T getResult() {
      accum.reset();
      synchronized (window) {
         window.forEach(value -> {
            accum.add(value);
         });
      }
      return accum.getResult();
   }

   //   /**
   //    * Is used to attach a real accumulator that will do the actuall accumulation over the values in the given time window.
   //    *
   //    * @return The actual accumulator.
   //    **/
   //   protected abstract Accumulator<T> getRealAccumulator();

   @SuppressWarnings("unchecked")
   @Override
   public void add(final T value) {
      window.add(value);
   }

   @Override
   public void reset() {
      window.clear();
   }
}
