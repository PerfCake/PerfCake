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
package org.perfcake.reporting.reporter.accumulator;

import org.perfcake.common.TimeSlidingWindow;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Accumulates a value over a set of recently reported values in a time sliding window.
 * The sliding window is a time period in milliseconds.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public abstract class AbstractTimeSlidingWindowAccumulator<T, A extends Accumulator<T>> implements Accumulator<T> {

   /**
    * Logger of this class.
    */
   private static final Logger log = LogManager.getLogger(AbstractTimeSlidingWindowAccumulator.class);

   /**
    * Sliding time window to record values of the given time period.
    */
   protected final TimeSlidingWindow<T> window;

   /**
    * Underlying accumulator class to calculate the result from values in the time window.
    */
   protected final Class<A> accumulatorClass;

   /**
    * Creates a new accumulator with the sliding window of a given time period.
    *
    * @param windowSize
    *       Size of the sliding window.
    * @param accumulatorClass
    *       Type of the underlying accumulator that is encapsulated in the sliding time window.
    */
   public AbstractTimeSlidingWindowAccumulator(final int windowSize, final Class<A> accumulatorClass) {
      window = new TimeSlidingWindow<>(windowSize);
      this.accumulatorClass = accumulatorClass;
   }

   @Override
   public T getResult() {
      final A accumulator = getNewAccumulator();

      if (accumulator != null) {
         window.forEach(accumulator::add);
         return accumulator.getResult();
      } else {
         return null;
      }
   }

   @SuppressWarnings("unchecked")
   @Override
   public void add(final T value) {
      window.add(value);
   }

   @Override
   public void reset() {
      window.clear();
   }

   /**
    * Gets a new accumulator instance.
    *
    * @return A new accumulator instance.
    */
   private A getNewAccumulator() {
      try {
         return accumulatorClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
         log.error("Unable to initialize the underlying accumulator: ", e);
      }

      return null;
   }
}
