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
package org.perfcake.reporting.reporter;

import org.perfcake.PerfCakeConst;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.BinaryScalableQuantity;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destination.Destination;
import org.perfcake.reporting.reporter.accumulator.Accumulator;
import org.perfcake.reporting.reporter.accumulator.AvgAccumulator;
import org.perfcake.reporting.reporter.accumulator.Histogram;
import org.perfcake.reporting.reporter.accumulator.LastValueAccumulator;
import org.perfcake.reporting.reporter.accumulator.MaxAccumulator;
import org.perfcake.reporting.reporter.accumulator.MinAccumulator;
import org.perfcake.reporting.reporter.accumulator.SlidingWindowAvgAccumulator;
import org.perfcake.reporting.reporter.accumulator.SlidingWindowMaxAccumulator;
import org.perfcake.reporting.reporter.accumulator.SlidingWindowMinAccumulator;
import org.perfcake.reporting.reporter.accumulator.SlidingWindowSumLongAccumulator;
import org.perfcake.reporting.reporter.accumulator.SumLongAccumulator;
import org.perfcake.reporting.reporter.accumulator.TimeSlidingWindowAvgAccumulator;
import org.perfcake.reporting.reporter.accumulator.TimeSlidingWindowMaxAccumulator;
import org.perfcake.reporting.reporter.accumulator.TimeSlidingWindowMinAccumulator;
import org.perfcake.reporting.reporter.accumulator.TimeSlidingWindowSumLongAccumulator;

/**
 * <p>Reports the minimal, maximal and average value from the beginning
 * of the measuring to the moment when the results are published including. The actual value about what
 * the statistics are gathered is computed as a result of the {@link #computeResult(MeasurementUnit)} method.</p>
 *
 * <p>The default value of the reporter is a current value at the moment of publishing.</p>
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
    * True when request size reporting is enabled.
    */
   private boolean requestSizeEnabled = true;

   /**
    * True when response size reporting is enabled.
    */
   private boolean responseSizeEnabled = true;

   /**
    * A property that specifies a window size with the default value of {@link Integer#MAX_VALUE}.
    */
   private int windowSize = Integer.MAX_VALUE;

   /**
    * The type of the window, either the number of iterations or a time duration.
    */
   private WindowType windowType = WindowType.ITERATION;

   /**
    * The type of the window, either the number of iterations or a time duration.
    */
   public enum WindowType {

      /**
       * A window of the number of latest iterations.
       */
      ITERATION,

      /**
       * A window of a given time period.
       */
      TIME
   }

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
      if (Double.class.equals(clazz) || PerfCakeConst.REQUEST_SIZE_TAG.equals(key) || PerfCakeConst.RESPONSE_SIZE_TAG.equals(key)) {
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
      switch (windowType) {
         case TIME:
            switch (key) {
               case MAXIMUM:
                  return new TimeSlidingWindowMaxAccumulator(windowSize);
               case MINIMUM:
                  return new TimeSlidingWindowMinAccumulator(windowSize);
               case Measurement.DEFAULT_RESULT:
                  return new LastValueAccumulator();
               case PerfCakeConst.REQUEST_SIZE_TAG:
               case PerfCakeConst.RESPONSE_SIZE_TAG:
                  return new TimeSlidingWindowSumLongAccumulator(windowSize);
               case AVERAGE:
               default:
                  return new TimeSlidingWindowAvgAccumulator(windowSize);
            }
         case ITERATION:
         default:
            switch (key) {
               case MAXIMUM:
                  return new SlidingWindowMaxAccumulator(windowSize);
               case MINIMUM:
                  return new SlidingWindowMinAccumulator(windowSize);
               case Measurement.DEFAULT_RESULT:
                  return new LastValueAccumulator();
               case PerfCakeConst.REQUEST_SIZE_TAG:
               case PerfCakeConst.RESPONSE_SIZE_TAG:
                  return new SlidingWindowSumLongAccumulator(windowSize);
               case AVERAGE:
               default:
                  return new SlidingWindowAvgAccumulator(windowSize);
            }
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
         case PerfCakeConst.REQUEST_SIZE_TAG:
         case PerfCakeConst.RESPONSE_SIZE_TAG:
            return new SumLongAccumulator();
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

      if (requestSizeEnabled) {
         wrapResultByScalableQuantity(m, PerfCakeConst.REQUEST_SIZE_TAG, "B");
      } else {
         m.remove(PerfCakeConst.REQUEST_SIZE_TAG);
      }

      if (responseSizeEnabled) {
         wrapResultByScalableQuantity(m, PerfCakeConst.RESPONSE_SIZE_TAG, "B");
      } else {
         m.remove(PerfCakeConst.RESPONSE_SIZE_TAG);
      }

      if (histogramCounter != null) {
         histogramCounter.getHistogramInPercent().forEach((range, value) -> m.set(histogramPrefix + range.toString(), new Quantity<>(value, "%")));
      }

      destination.report(m);
   }

   private void wrapResultByScalableQuantity(final Measurement measurement, final String key, final String unit) {
      final Long result = (Long) measurement.get(key);
      if (result != null) {
         measurement.set(key, new BinaryScalableQuantity(result, unit));
      }
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
    * <p>Sets the size of the sliding window.</p>
    *
    * <p>If the size is equal to {@link Integer#MAX_VALUE} (which is the default value), then it means the
    * sliding window is not used at all and the statistics are taken from the whole run.</p>
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
    * @return Instance of this to support fluent API.
    */
   public StatsReporter setHistogram(String histogram) {
      this.histogram = histogram;
      return this;
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
    * @return Instance of this to support fluent API.
    */
   public StatsReporter setHistogramPrefix(String histogramPrefix) {
      this.histogramPrefix = histogramPrefix;
      return this;
   }

   /**
    * Is the metric of the request size enabled?
    *
    * @return True if and only if the metric of the request size is enabled.
    */
   public boolean isRequestSizeEnabled() {
      return requestSizeEnabled;
   }

   /**
    * Enables and disables the metric of the request size.
    *
    * @param requestSizeEnabled
    *       True to enable the metric.
    * @return Instance of this to support fluent API.
    */
   public StatsReporter setRequestSizeEnabled(final boolean requestSizeEnabled) {
      this.requestSizeEnabled = requestSizeEnabled;
      return this;
   }

   /**
    * Is the metric of the response size enabled?
    *
    * @return True if and only if the metric of the response size is enabled.
    */
   public boolean isResponseSizeEnabled() {
      return responseSizeEnabled;
   }

   /**
    * Enables and disables the metric of the response size.
    *
    * @param responseSizeEnabled
    *       True to enable the metric.
    * @return Instance of this to support fluent API.
    */
   public StatsReporter setResponseSizeEnabled(final boolean responseSizeEnabled) {
      this.responseSizeEnabled = responseSizeEnabled;
      return this;
   }

   /**
    * Returns the type of the window.
    *
    * @return The window type.
    */
   public WindowType getWindowType() {
      return windowType;
   }

   /**
    * Sets the type of the window.
    *
    * @param windowType
    *       Either ITERATION or TIME is supported.
    * @return The instance of this for a fluent API.
    */
   public StatsReporter setWindowType(final WindowType windowType) {
      this.windowType = windowType;
      return this;
   }
}
