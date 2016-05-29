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
package org.perfcake.reporting.reporters;

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.AvgAccumulator;

import org.HdrHistogram.ConcurrentDoubleHistogram;
import org.HdrHistogram.DoubleHistogram;
import org.HdrHistogram.DoubleHistogramIterationValue;
import org.HdrHistogram.DoublePercentileIterator;

import java.util.Locale;

/**
 * Reports response time in an HDR Histogram that can computationally correct the Coordinated omission problem.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ResponseTimeHistogramReporter extends AbstractReporter {

   /**
    * Correction mode can be switched off (no correction), automatic or user specified.
    */
   public enum Correction {
      OFF, AUTO, USER
   }

   /**
    * Precision of the resulting histogram (number of significant digits).
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
    * The value of normal/typical/expected response time to correct the histogram
    * while the {@link Correction#USER} correction mode is turned on.
    */
   private double correction = 1d;

   /**
    * Prefix of the percentile keys in the result map.
    */
   private String prefix = "perc";

   /**
    * Detail level of the result. I.e. the scaling factor by which to divide histogram recorded values units in output.
    */
   private int detail = 2;

   /**
    * Accumulator to store average response rate for histogram auto-correction.
    */
   private AvgAccumulator avg = new AvgAccumulator();

   /**
    * Histogram instance to store the data.
    */
   private DoubleHistogram histogram = new ConcurrentDoubleHistogram(precision);

   /**
    * Format of the result value.
    */
   private String valueFormatString = "%12." + precision + "f";

   /**
    * Format of the percentile expression.
    */
   private static final String percentileFormatString = "%2.12f";

   @Override
   protected void doReset() {
      avg.reset();
      histogram.reset();
   }

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      avg.add(measurementUnit.getTotalTime());
      histogram.recordValue(measurementUnit.getTotalTime());
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination destination) throws ReportingException {
      final Measurement m = newMeasurement();

      DoublePercentileIterator pi;

      final DoubleHistogram localHistogram = histogram.copy();

      switch (correctionMode) {
         case AUTO:
            pi = new DoublePercentileIterator(localHistogram.copyCorrectedForCoordinatedOmission(avg.getResult()), 1);
            break;
         case USER:
            pi = new DoublePercentileIterator(localHistogram.copyCorrectedForCoordinatedOmission(correction), 1);
            break;
         default:
            pi = new DoublePercentileIterator(localHistogram, 1);
      }

      pi.reset(detail);

      while (pi.hasNext()) {
         DoubleHistogramIterationValue val = pi.next();

         m.set(prefix + String.format(Locale.US, percentileFormatString, val.getPercentileLevelIteratedTo() / 100d),
               String.format(Locale.US, valueFormatString, val.getValueIteratedTo()));
      }

      destination.report(m);
   }

   /**
    * Gets the precision as the number of significant digits that are distinguished by this reporter.
    *
    * @return The number of significant digits that are distinguished by this reporter.
    */
   public int getPrecision() {
      return precision;
   }

   /**
    * Sets the precision as the number of significant digits that are distinguished by this reporter.
    *
    * @param precision
    *       The number of significant digits that are distinguished by this reporter.
    */
   public void setPrecision(final int precision) {
      this.precision = precision;
   }

   /**
    * Gets the correction of coordinated omission in the resulting histogram.
    * {@link Correction#AUTO} is the default value and this means that the histogram is corrected
    * by the average measured value. In case of the {@link Correction#USER} mode, the user specifies
    * the expected response time manually for correct computation of the histogram. When the
    * {@link Correction#OFF} mode is used, no correction is done.
    *
    * @return The correction mode.
    */
   public Correction getCorrectionMode() {
      return correctionMode;
   }

   /**
    * Sets the correction of coordinated omission in the resulting histogram.
    * {@link Correction#AUTO} is the default value and this means that the histogram is corrected
    * by the average measured value. In case of the {@link Correction#USER} mode, the user specifies
    * the expected response time manually for correct computation of the histogram. When the
    * {@link Correction#OFF} mode is used, no correction is done.
    *
    * @param correctionMode
    *       The correction mode to be used.
    */
   public void setCorrectionMode(final Correction correctionMode) {
      this.correctionMode = correctionMode;
   }

   /**
    * Gets the correction value for the coordinated omission when the {@link Correction#USER} mode is set.
    *
    * @return The correction value.
    */
   public double getCorrection() {
      return correction;
   }

   /**
    * Sets the correction value for the coordinated omission when the {@link Correction#USER} mode is set.
    *
    * @param correction
    *       The correction value.
    */
   public void setCorrection(final double correction) {
      this.correction = correction;
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
    */
   public void setPrefix(final String prefix) {
      this.prefix = prefix;
   }

   /**
    * Gets the detail level of the result. I.e. the scaling factor by which to divide histogram recorded values units in output.
    *
    * @return The detail level.
    */
   public int getDetail() {
      return detail;
   }

   /**
    * Sets the detail level of the result. I.e. the scaling factor by which to divide histogram recorded values units in output.
    *
    * @param detail
    *       The detail level.
    */
   public void setDetail(final int detail) {
      this.detail = detail;
   }
}
