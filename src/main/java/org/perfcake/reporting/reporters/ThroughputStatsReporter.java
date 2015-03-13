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

import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.HarmonicMeanAccumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowHarmonicMeanAccumulator;

/**
 * Reports statistics of throughput.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @see StatsReporter
 */
public class ThroughputStatsReporter extends StatsReporter {

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getWindowedAccumulator(final String key) {
      if (AVERAGE.equals(key)) {
         return new SlidingWindowHarmonicMeanAccumulator(getWindowSize());
      } else {
         return super.getWindowedAccumulator(key);
      }
   }

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getNonWindowedAccumulator(final String key) {
      if (AVERAGE.equals(key)) {
         return new HarmonicMeanAccumulator();
      } else {
         return super.getNonWindowedAccumulator(key);
      }
   }

   @Override
   protected Double computeResult(final MeasurementUnit measurementUnit) {
      return 1000d * runInfo.getThreads() / measurementUnit.getLastTime(); // per second
   }

   @Override
   protected String getResultUnit() {
      return "iterations/s";
   }
}
