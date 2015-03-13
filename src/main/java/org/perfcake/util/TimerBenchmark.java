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
package org.perfcake.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Measures the resolution of System.nanoTime() in the current system.
 * Purpose of this is purely informative to the end user.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class TimerBenchmark {

   private static final Logger log = LogManager.getLogger(TimerBenchmark.class);

   /**
    * Last benchmark result.
    */
   private static long lastDelta = Long.MAX_VALUE;

   /**
    * Benchmark cycles.
    */
   private static final int CYCLES = 10 * 1024;

   /**
    * Measures system timer resolution.
    **/
   public static void measureTimerResolution() {
      final long results[] = new long[CYCLES];

      log.info("Benchmarking system timer resolution...");

      for (int i = 0; i < CYCLES; i++) {
         results[i] = System.nanoTime();
      }

      long delta = Long.MAX_VALUE;
      final long prev = results[0];
      for (int i = 1; i < CYCLES; i++) {
         if (results[i] - prev < delta && results[i] - prev > 0) {
            delta = results[i] - prev;
         }
      }

      if (delta == Long.MAX_VALUE) {
         log.warn("Unable to measure system timer resolution! It is likely that PerfCake will not provide reasonable results.");
      }

      lastDelta = delta;

      log.info(String.format("This system is able to differentiate up to %dns. A single thread is now able to measure maximum of %d iterations/second.", delta, 1_000_000_000 / delta));
   }

   /**
    * Gets the last measured resolution of the system timer.
    *
    * @return The smallest amount of nano-seconds this system is able to differentiate.
    */
   public static long getLastDelta() {
      return lastDelta;
   }
}
