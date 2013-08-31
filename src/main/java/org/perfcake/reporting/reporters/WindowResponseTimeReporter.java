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
package org.perfcake.reporting.reporters;

import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowAvgAccumulator;

/**
 * A reporter that measures average response time over a given period of time.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class WindowResponseTimeReporter extends ResponseTimeReporter {

   /**
    * Sliding window size
    */
   private int windowSize = 16;

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(final String key, final Class clazz) {
      if (Double.class.getCanonicalName().equals(clazz.getCanonicalName())) {
         return new SlidingWindowAvgAccumulator(windowSize);
      } else {
         return super.getAccumulator(key, clazz);
      }
   }

   /**
    * Gets the current window size.
    * 
    * @return Current window size set to this report. Note that this might be a different size from the one actually in use if the size was changed
    *         after the measurement started.
    */
   public int getWindowSize() {
      return windowSize;
   }

   /**
    * Sets the size of a sliding window used to accumulate results.
    * This only takes effect at the start of a new measurement. During the actual measurement (after a call to {@link #start()} method) it does not have any effect.
    * Another call to {@link #start()} or {@link #reset()} must be done.
    * 
    * @param windowSize
    *           Size of the sliding window
    */
   public void setWindowSize(final int windowSize) {
      this.windowSize = windowSize;
   }

}
