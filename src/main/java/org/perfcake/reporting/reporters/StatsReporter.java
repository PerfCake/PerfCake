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

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.AvgAccumulator;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;
import org.perfcake.reporting.reporters.accumulators.MaxAccumulator;
import org.perfcake.reporting.reporters.accumulators.MinAccumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowAvgAccumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowMaxAccumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowMinAccumulator;

/**
 * This abstract reporter is able to report the minimal, maximal and average value from the beginning
 * of the measuring to the moment when the results are published including. The actual value about what
 * the statistics are gathered is computed as a result of the {@link #computeResult(MeasurementUnit)} method.
 * 
 * The default value of the reporter is a current value at the moment of publishing.
 * 
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public abstract class StatsReporter extends AbstractReporter {

   /**
    * A property that determines if the metric of a maximal value is enabled or disabled.
    */
   private boolean maximumEnabled = true;

   /**
    * A property that determines if the metric of a minimal value is enabled or disabled.
    */
   private boolean minimumEnabled = true;

   /**
    * A property that determines if the metric of an average value is enabled or disabled.
    */
   private boolean averageEnabled = true;

   /**
    * A property that specifies a window size with the default value of {@link Integer#MAX_VALUE}.
    */
   private int windowSize = Integer.MAX_VALUE;

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
         if (windowSize == Integer.MAX_VALUE) {
            return getNonWindowedAccumulator(key);
         } else {
            return getWindowedAccumulator(key);
         }
      }
      return super.getAccumulator(key, clazz);
   }

   /**
    * Gets an appropriate accumulator for a given key from the Measurement Unit's results map for the case that the value of the {@link #windowSize} is
    * different from the default value of {@link Integer#MAX_VALUE}.
    * 
    * @param key
    *        Name of the key from the results map.
    * @return An appropriate accumulator instance.
    */
   @SuppressWarnings("rawtypes")
   protected Accumulator getWindowedAccumulator(String key) {
      switch (key) {
         case MAXIMUM:
            return new SlidingWindowMaxAccumulator(windowSize);
         case MINIMUM:
            return new SlidingWindowMinAccumulator(windowSize);
         case Measurement.DEFAULT_RESULT:
            return new LastValueAccumulator();
         case AVERAGE:
         default:
            return new SlidingWindowAvgAccumulator(windowSize);
      }
   }

   /**
    * Gets an appropriate accumulator for a given key from the Measurement Unit's results map for the case that the value of the {@link #windowSize} is
    * equal to the value of {@link Integer#MAX_VALUE}, which is the default value.
    * 
    * @param key
    *        Name of the key from the results map.
    * @return An appropriate accumulator instance.
    */
   @SuppressWarnings("rawtypes")
   protected Accumulator getNonWindowedAccumulator(String key) {
      switch (key) {
         case MAXIMUM:
            return new MaxAccumulator();
         case MINIMUM:
            return new MinAccumulator();
         case Measurement.DEFAULT_RESULT:
            return new LastValueAccumulator();
         case AVERAGE:
         default:
            return new AvgAccumulator();
      }
   }

   /**
    * Computes the actual result value about what the reporter will collect the statistics.
    */
   protected abstract Double computeResult(final MeasurementUnit mu);

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      final Double result = computeResult(mu);

      mu.appendResult(Measurement.DEFAULT_RESULT, result);

      if (averageEnabled) {
         mu.appendResult(AVERAGE, result);
      }

      if (minimumEnabled) {
         mu.appendResult(MINIMUM, result);
      }

      if (maximumEnabled) {
         mu.appendResult(MAXIMUM, result);
      }
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      final Measurement m = newMeasurement();
      publishAccumulatedResult(m);
      final String unit = getResultUnit();
      if (unit != null) {
         wrapResultByQuantity(m, Measurement.DEFAULT_RESULT, unit);
         wrapResultByQuantity(m, AVERAGE, unit);
         wrapResultByQuantity(m, MINIMUM, unit);
         wrapResultByQuantity(m, MAXIMUM, unit);
      }
      d.report(m);
   }

   private void wrapResultByQuantity(final Measurement m, final String key, final String unit) {
      final Number result = (Number) m.get(key);
      if (result != null) {
         m.set(key, new Quantity<Number>(result, unit));
      }
   }

   @Override
   protected void doReset() {
      // nothing needed, the parent does the job
   }

   /**
    * Gets the status of the metric of a maximal value
    * 
    * @return Returns <code>true</code> if the metric of a maximal is enabled or <code>false</code> otherwise.
    */
   public boolean isMaximumEnabled() {
      return maximumEnabled;
   }

   /**
    * Enables or disables the metric of a maximal value.
    * 
    * @param maximumEnabled
    *        Set <code>true</code> to enable the metric of a maximal value or <code>false</code> to disable it.
    */
   public StatsReporter setMaximumEnabled(boolean maximumEnabled) {
      this.maximumEnabled = maximumEnabled;
      return this;
   }

   /**
    * Gets the status of the metric of a minimal value.
    * 
    * @return Returns <code>true</code> if the metric of a minimal value is enabled or <code>false</code> otherwise.
    */
   public boolean isMinimumEnabled() {
      return minimumEnabled;
   }

   /**
    * Enables or disables the metric of a minimal value.
    * 
    * @param minimumEnabled
    *        Set <code>true</code> to enable the metric of a minimal value or <code>false</code> to disable it.
    */
   public StatsReporter setMinimumEnabled(boolean minimumEnabled) {
      this.minimumEnabled = minimumEnabled;
      return this;
   }

   /**
    * Gets the status of the metric of an average value.
    * 
    * @return Returns <code>true</code> if the metric of an average value is enabled or <code>false</code> otherwise.
    */
   public boolean isAverageEnabled() {
      return averageEnabled;
   }

   /**
    * Enables or disables the metric of an average value.
    * 
    * @param averageEnabled
    *        Set <code>true</code> to enable the metric of an average value or <code>false</code> to disable it.
    */
   public StatsReporter setAverageEnabled(boolean averageEnabled) {
      this.averageEnabled = averageEnabled;
      return this;
   }

   /**
    * Gets the sliding window size if set. If the size is equal to {@link Integer#MAX_VALUE}, then it means the
    * sliding window is not used at all and the statistics are taken from the whole run.
    * 
    * @return The sliding window size.
    */
   public int getWindowSize() {
      return windowSize;
   }

   /**
    * Sets the size of the sliding window.
    * 
    * If the size is equal to {@link Integer#MAX_VALUE} (which is the default value), then it means the
    * sliding window is not used at all and the statistics are taken from the whole run.
    * 
    * @param windowSize
    *        The sliding window size.
    */
   public StatsReporter setWindowSize(int windowSize) {
      this.windowSize = windowSize;
      return this;
   }

   /**
    * Returns the unit of the results.
    * If it returns a value that is not <code>null</code> the method {@link #publishResult(PeriodType, Destination)} will wrap the reporter's results by a {@link Quantity} with the return value of
    * this method as units before sent to destinations.
    * If a stats reporter is supposed to have results with units the reporter is to override this method.<br/>
    * If the method returns a <code>null</code> value, the reporter's results remain untouched.
    * 
    * @return The string representation of the reporter results' unit or a <code>null</code> value.
    **/
   protected String getResultUnit() {
      return null;
   }
}
