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

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destination.Destination;
import org.perfcake.reporting.reporter.accumulator.AvgAccumulator;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.PercentileIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * <p>Reports response time in milliseconds using <a href="https://github.com/HdrHistogram/HdrHistogram">HDR Histogram</a> that can
 * computationally correct the Coordinated omission problem.</p>
 *
 * <p>The following paragraphs are based on <a href="https://github.com/HdrHistogram/HdrHistogram/blob/master/README.md">the HDR Histogram documentation</a>.</p>
 *
 * <p>This reporter depends on the features introduced by HDR Histogram to correct the coordinated omission.
 * To compensate for the loss of sampled values when a recorded value is larger than the expected,
 * interval between value samples, HDR Histogram will auto-generate an additional series of decreasingly-smaller value records.
 * The values go down to the {@link #expectedValue} in case of the {@link Correction#USER} correction mode, or down to the average response time in
 * case of the {@link Correction#AUTO} correction mode.</p>
 *
 * <p>The reporter could be configured to track the counts of observed response times in milliseconds between 0 and 3,600,000
 * ({@link #maxExpectedValue}) while maintaining a value precision of 3 ({@link #precision}) significant digits across that range.
 * Value quantization within the range will thus be no larger than 1/1,000th (or 0.1%) of any value. This example reporter could be used to track
 * and analyze the counts of observed response times ranging between 1 millisecond and 1 hour in magnitude, while maintaining a value resolution
 * of 1 millisecond (or better) up to one second, and a resolution of 1 second (or better) up to 1,000 seconds.
 * At its maximum tracked value (1 hour), it would still maintain a resolution of 3.6 seconds (or better).</p>
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ResponseTimeHistogramReporter extends AbstractReporter {

   /**
    * The reporter's logger.
    */
   private static final Logger log = LogManager.getLogger(ResponseTimeHistogramReporter.class);

   /**
    * Correction mode can be switched off (no correction), automatic or user specified.
    */
   public enum Correction {
      OFF, AUTO, USER
   }

   /**
    * Precision of the resulting histogram (number of significant digits) in range 0 - 5.
    * This determines the memory used by the reporter.
    */
   private int precision = 2;

   /**
    * The correction of coordinated omission in the resulting histogram.
    * {@link Correction#AUTO} is the default value and this means that the histogram is corrected
    * by the average measured value.
    */
   private Correction correctionMode = Correction.AUTO;

   /**
    * The value of normal/typical/expected response time in ms to correct the histogram
    * while the {@link Correction#USER} correction mode is turned on.
    */
   private long expectedValue = 1L;

   /**
    * Prefix of the percentile keys in the result map.
    */
   private String prefix = "perc";

   /**
    * Detail level of the result (the number of iteration steps per half-distance to 100%).
    * Must be greater than 0.
    */
   private int detail = 2;

   /**
    * The maximum expected value to better organize the data in the histogram. The response time
    * reported must never exceed this value, otherwise the result will be skipped, an error reported and the
    * output will be invalid. -1 turns the optimization off. It is valuable to set some reasonable number like
    * 3_600_000 which equals to the resolution from 1 millisecond to 1 hour.
    */
   private long maxExpectedValue = -1;

   /**
    * When set to true, the results are filter to keep just unique values.
    */
   private boolean filter = false;

   /**
    * Accumulator to store average response rate for histogram auto-correction.
    */
   private AvgAccumulator avg = new AvgAccumulator();

   /**
    * Histogram instance to store the data.
    */
   private Histogram histogram;

   /**
    * Format of the percentile expression.
    */
   private static final String percentileFormatString = "%2.12f";

   @Override
   protected void doReset() {
      avg.reset();
      initRecorder();
   }

   /**
    * Initializes a new histogram based on the configuration proeprties.
    */
   private void initRecorder() {
      if (maxExpectedValue == -1) {
         histogram = new Histogram(precision);
      } else {
         histogram = new Histogram(maxExpectedValue, precision);
      }
   }

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      avg.add(measurementUnit.getTotalTime());
      long responseTime = Math.round(measurementUnit.getTotalTime());

      if (maxExpectedValue != -1 && responseTime > maxExpectedValue) {
         log.error(String.format("Reported response time (%d) exceeds maximal trackable value (%d). Ignoring the value. Results are tampered!", responseTime, maxExpectedValue));
      } else {
         histogram.recordValue(responseTime);
      }
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination destination) throws ReportingException {
      final Measurement m = newMeasurement();
      publishAccumulatedResult(m);

      PercentileIterator pi;

      Histogram localHistogram = histogram.copy();

      switch (correctionMode) {
         case AUTO:
            pi = new PercentileIterator(localHistogram.copyCorrectedForCoordinatedOmission(Math.round(avg.getResult())), detail);
            break;
         case USER:
            pi = new PercentileIterator(localHistogram.copyCorrectedForCoordinatedOmission(expectedValue), detail);
            break;
         default:
            pi = new PercentileIterator(localHistogram, detail);
      }

      String lastKey = null;
      String lastValue = null;

      while (pi.hasNext()) {
         HistogramIterationValue val = pi.next();

         String key = prefix + String.format(Locale.US, percentileFormatString, val.getPercentileLevelIteratedTo() / 100d);
         String value = String.format(Locale.US, "%d", val.getValueIteratedTo());

         if (filter) {
            if (lastValue != null) {
               if (!value.equals(lastValue)) {
                  m.set(lastKey, lastValue);
               } else if (!pi.hasNext()) {
                  m.set(key, value);
               }
            }
            lastKey = key;
            lastValue = value;
         } else {
            m.set(key, value);
         }
      }

      destination.report(m);
   }

   /**
    * Gets the precision as the number of significant digits that are recognized by this reporter.
    *
    * @return The number of significant digits that are recognized by this reporter.
    */
   public int getPrecision() {
      return precision;
   }

   /**
    * Sets the precision as the number of significant digits that are recognized by this reporter.
    * Must be in interval 0 - 5. Default value is 2.
    *
    * @param precision
    *       The number of significant digits that are recognized by this reporter.
    * @return Instance of this to support fluent API.
    */
   public ResponseTimeHistogramReporter setPrecision(final int precision) {
      if (precision < 0 || precision > 5) {
         log.warn(String.format("Wrong level of precision set (%d). Keeping the original value (%d).", precision, this.precision));
      } else {
         this.precision = precision;
         initRecorder();
      }

      return this;
   }

   /**
    * Gets the expectedValue of coordinated omission in the resulting histogram.
    * {@link Correction#AUTO} is the default value and this means that the histogram is corrected
    * by the average measured value. In case of the {@link Correction#USER} mode, the user specifies
    * the expected response time manually for correct computation of the histogram. When the
    * {@link Correction#OFF} mode is used, no correction is performed.
    *
    * @return The expectedValue mode.
    */
   public Correction getCorrectionMode() {
      return correctionMode;
   }

   /**
    * Sets the expectedValue of coordinated omission in the resulting histogram.
    * {@link Correction#AUTO} is the default value and this means that the histogram is corrected
    * by the average measured value. In case of the {@link Correction#USER} mode, the user specifies
    * the expected response time manually for correct computation of the histogram. When the
    * {@link Correction#OFF} mode is used, no correction is performed.
    *
    * @param correctionMode
    *       The expectedValue mode to be used.
    * @return Instance of this to support fluent API.
    */
   public ResponseTimeHistogramReporter setCorrectionMode(final Correction correctionMode) {
      this.correctionMode = correctionMode;
      return this;
   }

   /**
    * Gets the expectedValue value for the coordinated omission when the {@link Correction#USER} mode is set.
    *
    * @return The expectedValue value.
    */
   public long getExpectedValue() {
      return expectedValue;
   }

   /**
    * Sets the expectedValue value for the coordinated omission when the {@link Correction#USER} mode is set.
    *
    * @param expectedValue
    *       The expectedValue value.
    * @return Instance of this to support fluent API.
    */
   public ResponseTimeHistogramReporter setExpectedValue(final long expectedValue) {
      this.expectedValue = expectedValue;
      return this;
   }

   /**
    * Gets the prefix of percentile values in the result map.
    *
    * @return The percentile value prefix.
    */
   public String getPrefix() {
      return prefix;
   }

   /**
    * Sets the prefix of percentile values in the result map.
    *
    * @param prefix
    *       The percentile value prefix.
    * @return Instance of this to support fluent API.
    */
   public ResponseTimeHistogramReporter setPrefix(final String prefix) {
      this.prefix = prefix;
      return this;
   }

   /**
    * Gets the detail level of the result (the number of iteration steps per half-distance to 100%).
    * Must be greater than 0. The default value is 2.
    *
    * @return The detail level.
    */
   public int getDetail() {
      return detail;
   }

   /**
    * Sets the detail level of the result (the number of iteration steps per half-distance to 100%).
    * Must be greater than 0. The default value is 2.
    *
    * @param detail
    *       The detail level.
    * @return Instance of this to support fluent API.
    */
   public ResponseTimeHistogramReporter setDetail(final int detail) {
      if (detail < 1) {
         log.warn(String.format("Wrong level of detail set (%d). Keeping the original value (%d).", detail, this.detail));
      } else {
         this.detail = detail;
      }

      return this;
   }

   /**
    * Gets the maximum expected value to better organize the data in the histogram.
    *
    * @return The maximal expected value. -1 means that this optimization off.
    */
   public long getMaxExpectedValue() {
      return maxExpectedValue;
   }

   /**
    * Sets the maximum expected value to better organize the data in the histogram. The response time
    * reported must never exceed this value, otherwise the result will be skipped, an error reported and the
    * output will be invalid. -1 turns the optimization off. It is valuable to set some reasonable number like
    * 3_600_000 which equals to the resolution from 1 millisecond to 1 hour.
    *
    * @param maxExpectedValue
    *       The maximal expected value. -1 to turn this optimization off. -1 is the default value.
    * @return Instance of this to support fluent API.
    */
   public ResponseTimeHistogramReporter setMaxExpectedValue(final long maxExpectedValue) {
      this.maxExpectedValue = maxExpectedValue;
      initRecorder();
      return this;
   }

   /**
    * Gets the state of results filter. When true, the results with the same value are collapsed.
    *
    * @return The state of results filter. When true, the results with the same value are collapsed.
    */
   public boolean isFilter() {
      return filter;
   }

   /**
    * Sets the state of results filter. When true, the results with the same value are collapsed.
    *
    * @param filter
    *       The state of results filter. When true, the results with the same value are collapsed.
    * @return Instance of this to support fluent API.
    */
   public ResponseTimeHistogramReporter setFilter(final boolean filter) {
      this.filter = filter;
      return this;
   }
}
