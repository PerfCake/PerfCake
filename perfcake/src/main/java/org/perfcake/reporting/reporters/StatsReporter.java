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
import org.perfcake.reporting.reporters.accumulators.Histogram;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;
import org.perfcake.reporting.reporters.accumulators.MaxAccumulator;
import org.perfcake.reporting.reporters.accumulators.MinAccumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowAvgAccumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowMaxAccumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowMinAccumulator;

/**
 * Reports the minimal, maximal and average value from the beginning
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
    * A comma separated list of values where the histogram is split to individual ranges.
    */
   private String histogram = "";

   /**
    * String prefix used in the result map for histogram entries. This prefix is followed by the mathematical representation of the particular range.
    */
   private String histogramPrefix = "in";

   /**
    * The actual histogram representation.
    */
   private Histogram histogramCounter = null;

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
   protected Accumulator getAccumulator(final String key, final Class clazz) {
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
    *       Name of the key from the results map.
    * @return An appropriate accumulator instance.
    */
   @SuppressWarnings("rawtypes")
   protected Accumulator getWindowedAccumulator(final String key) {
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
    *       Name of the key from the results map.
    * @return An appropriate accumulator instance.
    */
   @SuppressWarnings("rawtypes")
   protected Accumulator getNonWindowedAccumulator(final String key) {
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
    *
    * @param measurementUnit
    *       Provided {@link MeasurementUnit} with all the measured values.
    * @return The processed result based on the input {@link MeasurementUnit}.
    */
   protected abstract Double computeResult(final MeasurementUnit measurementUnit);

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      final Double result = computeResult(measurementUnit);

      measurementUnit.appendResult(Measurement.DEFAULT_RESULT, result);

      if (averageEnabled) {
         measurementUnit.appendResult(AVERAGE, result);
      }

      if (minimumEnabled) {
         measurementUnit.appendResult(MINIMUM, result);
      }

      if (maximumEnabled) {
         measurementUnit.appendResult(MAXIMUM, result);
      }

      if (histogramCounter != null) {
         histogramCounter.add(result);
      }
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination destination) throws ReportingException {
      final Measurement m = newMeasurement();
      publishAccumulatedResult(m);
      final String unit = getResultUnit();
      if (unit != null) {
         wrapResultByQuantity(m, Measurement.DEFAULT_RESULT, unit);
         wrapResultByQuantity(m, AVERAGE, unit);
         wrapResultByQuantity(m, MINIMUM, unit);
         wrapResultByQuantity(m, MAXIMUM, unit);
      }

      if (histogramCounter != null) {
         histogramCounter.getHistogramInPercent().forEach((range, value) -> m.set(histogramPrefix + range.toString(), value));
      }

      destination.report(m);
   }

   private void wrapResultByQuantity(final Measurement measurement, final String key, final String unit) {
      final Number result = (Number) measurement.get(key);
      if (result != null) {
         measurement.set(key, new Quantity<>(result, unit));
      }
   }

   @Override
   protected void doReset() {
      if (histogram != null && !histogram.isEmpty()) {
         histogramCounter = new Histogram(histogram);
      }
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
    *       Set <code>true</code> to enable the metric of a maximal value or <code>false</code> to disable it.
    * @return Instance of this for fluent API.
    */
   public StatsReporter setMaximumEnabled(final boolean maximumEnabled) {
      this.maximumEnabled = maximumEnabled;
      return this;
   }

   /**
    * Gets the status of the metric of a minimal value.
    *
    * @return <code>true</code> if the metric of a minimal value is enabled or <code>false</code> otherwise.
    */
   public boolean isMinimumEnabled() {
      return minimumEnabled;
   }

   /**
    * Enables or disables the metric of a minimal value.
    *
    * @param minimumEnabled
    *       <code>true</code> to enable the metric of a minimal value or <code>false</code> to disable it.
    * @return Instance of this for fluent API.
    */
   public StatsReporter setMinimumEnabled(final boolean minimumEnabled) {
      this.minimumEnabled = minimumEnabled;
      return this;
   }

   /**
    * Gets the status of the metric of an average value.
    *
    * @return <code>true</code> if the metric of an average value is enabled or <code>false</code> otherwise.
    */
   public boolean isAverageEnabled() {
      return averageEnabled;
   }

   /**
    * Enables or disables the metric of an average value.
    *
    * @param averageEnabled
    *       <code>true</code> to enable the metric of an average value or <code>false</code> to disable it.
    * @return Instance of this for fluent API.
    */
   public StatsReporter setAverageEnabled(final boolean averageEnabled) {
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
    *       The sliding window size.
    * @return Instance of this for fluent API.
    */
   public StatsReporter setWindowSize(final int windowSize) {
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
    */
   protected String getResultUnit() {
      return null;
   }

   /**
    * Gets the string specifying where the histogram should be split.
    *
    * @return The string specifying where the histogram should be split.
    */
   public String getHistogram() {
      return histogram;
   }

   /**
    * Sets the string specifying where the histogram should be split.
    *
    * @param histogram
    *       The string specifying where the histogram should be split.
    */
   public void setHistogram(String histogram) {
      this.histogram = histogram;
   }

   /**
    * Gets the string prefix used in the result map for histogram entries.
    *
    * @return The string prefix used in the result map for histogram entries.
    */
   public String getHistogramPrefix() {
      return histogramPrefix;
   }

   /**
    * Sets the string prefix used in the result map for histogram entries.
    *
    * @param histogramPrefix
    *       The string prefix used in the result map for histogram entries.
    */
   public void setHistogramPrefix(String histogramPrefix) {
      this.histogramPrefix = histogramPrefix;
   }
}
