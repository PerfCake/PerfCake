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

import org.apache.log4j.Logger;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.HarmonicMeanAccumulator;

/**
 * The reporter is able to report statistics of throughput.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 * @see StatsReporter Details about the actual statistic metrics.
 */
public class ThroughputStatsReporter extends StatsReporter {

   /**
    * The logger.
    */
   private static final Logger log = Logger.getLogger(ThroughputStatsReporter.class);

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(String key, Class clazz) {
      if (AVERAGE.equals(key)) {
         return new HarmonicMeanAccumulator();
      } else {
         return super.getAccumulator(key, clazz);
      }
   }

   @Override
   protected Double computeResult(MeasurementUnit mu) {
      double tm = mu.getLastTime();
      if (tm == 0) {
         log.error("Unable to properly measure the time. The system is too fast. Run Info status: " + runInfo.toString());
      }
      return 1000d * runInfo.getThreads() / tm; // per second
   }
}
