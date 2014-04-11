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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.AvgAccumulator;
import org.perfcake.reporting.reporters.accumulators.MaxAccumulator;
import org.perfcake.reporting.reporters.accumulators.MinAccumulator;

/**
 * This abstract reporter is able to report the minimal, maximal and average value from the beginning
 * of the measuring to the moment when the results are published including a current value at the moment of publishing
 * as the default result.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public abstract class StatsReporter extends AbstractReporter {

   private boolean maximumEnabled = true;
   private boolean minimumEnabled = true;
   private boolean averageEnabled = true;

   /**
    * A String representation of a metric of a maximal value.
    */
   public static final String MAXIMUM = "Maximum";

   /**
    * A String representation of a metric of a minimal value.
    */
   public static final String MINIMUM = "Minimum";

   /**
    * A String representation of a metric of an average value.
    */
   public static final String AVERAGE = "Average";

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(String key, Class clazz) {
      if (Double.class.equals(clazz)) {
         switch (key) {
            case MAXIMUM:
               return new MaxAccumulator();
            case MINIMUM:
               return new MinAccumulator();
            case AVERAGE:
            default:
               return new AvgAccumulator();
         }
      }
      return super.getAccumulator(key, clazz);
   }

   /**
    * Computes the actual result value about what the reporter will collect the statistics.
    */
   protected abstract Object computeResult(final MeasurementUnit mu);

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      final Map<String, Object> results = new HashMap<>();
      final Object result = computeResult(mu);

      for (Entry<String, Object> entry : mu.getResults().entrySet()) {
         results.put(entry.getKey(), entry.getValue());
      }

      results.put(Measurement.DEFAULT_RESULT, result);

      if (averageEnabled) {
         results.put(AVERAGE, result);
      }

      if (minimumEnabled) {
         results.put(MINIMUM, result);
      }

      if (maximumEnabled) {
         results.put(MAXIMUM, result);
      }
      accumulateResults(results);
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      final Measurement m = newMeasurement();
      publishAccumulatedResult(m);
      d.report(m);
   }

   @Override
   protected void doReset() {
      // nothing needed, the parent does the job
   }

   /**
    * Used to read the value of maximumEnabled.
    * 
    * @return The maximumEnabled value.
    */
   public boolean isMaximumEnabled() {
      return maximumEnabled;
   }

   /**
    * Used to set the value of maximumEnabled.
    * 
    * @param maximumEnabled
    *           The maximumEnabled value to set.
    */
   public void setMaximumEnabled(boolean maximumEnabled) {
      this.maximumEnabled = maximumEnabled;
   }

   /**
    * Used to read the value of minimumEnabled.
    * 
    * @return The minimumEnabled value.
    */
   public boolean isMinimumEnabled() {
      return minimumEnabled;
   }

   /**
    * Used to set the value of minimumEnabled.
    * 
    * @param minimumEnabled
    *           The minimumEnabled value to set.
    */
   public void setMinimumEnabled(boolean minimumEnabled) {
      this.minimumEnabled = minimumEnabled;
   }

   /**
    * Used to read the value of averageEnabled.
    * 
    * @return The averageEnabled value.
    */
   public boolean isAverageEnabled() {
      return averageEnabled;
   }

   /**
    * Used to set the value of averageEnabled.
    * 
    * @param averageEnabled
    *           The averageEnabled value to set.
    */
   public void setAverageEnabled(boolean averageEnabled) {
      this.averageEnabled = averageEnabled;
   }
}
