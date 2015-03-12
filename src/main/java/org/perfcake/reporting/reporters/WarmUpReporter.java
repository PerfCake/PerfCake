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

import org.perfcake.PerfCakeConst;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowAvgAccumulator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Determines when the tested system is warmed up. The warming is enabled/disabled by the presence of the {@link WarmUpReporter} in the scenario. The minimal iteration count and
 * the warm-up period duration can be tweaked by the respective properties ({@link #minimalWarmUpCount} with the default value of 10,000 and {@link #minimalWarmUpDuration} with the default value of
 * 15,000 ms).
 * <p>
 * The system is considered warmed up when all of the following conditions are satisfied: The iteration length is not changing much over the time, the minimal iteration count has been executed and the
 * minimal duration from the very start has exceeded.
 * </p>
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class WarmUpReporter extends AbstractReporter {

   /**
    * The reporter's logger.
    */
   private static final Logger log = LogManager.getLogger(WarmUpReporter.class);

   /**
    * Minimal warm-up period duration in milliseconds.
    */
   private long minimalWarmUpDuration = 15000; // default 15s

   /**
    * Minimal iteration count executed during the warm-up period.
    */
   private long minimalWarmUpCount = 10000; // by JIT

   /**
    * The relative difference threshold to determine whether the throughput is not changing much.
    */
   private double relativeThreshold = 0.002d; // 0.2%

   /**
    * The absolute difference threshold to determine whether the throughput is not changing much.
    */
   private double absoluteThreshold = 0.2d; // 0.2

   /**
    * The flag indicating whether the tested system is considered warmed up.
    */
   private boolean warmed = false;

   /**
    * The index number of the checking period in which the current run is.
    *
    * @see #CHECKING_PERIOD
    */
   private final AtomicLong checkingPeriodIndex = new AtomicLong(0);

   private final LastValueAccumulator lastThroughput = new LastValueAccumulator();

   /**
    * The period in milliseconds in which the checking if the tested system is warmed up.
    */
   private final static long CHECKING_PERIOD = 1000;

   @Override
   public void start() {
      runInfo.addTag(PerfCakeConst.WARM_UP_TAG);
      if (log.isInfoEnabled()) {
         log.info("Warming the tested system up (for at least " + minimalWarmUpDuration + " ms and " + minimalWarmUpCount + " iterations) ...");
      }
      super.start();
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination destination) throws ReportingException {
      throw new ReportingException("No destination is allowed on " + getClass().getSimpleName());
   }

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(final String key, final Class clazz) {
      return new SlidingWindowAvgAccumulator(16);
   }

   @Override
   protected void doReset() {
      // nothing to do
   }

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      if (!warmed) {
         if (runInfo.getRunTime() / CHECKING_PERIOD > checkingPeriodIndex.get()) { // make sure we are in the next time interval and we should check for warm up end
            checkingPeriodIndex.incrementAndGet();

            // The throughput unit is number of iterations per second
            final Double currentThroughput = (double) CHECKING_PERIOD * getMaxIteration() / runInfo.getRunTime();
            final Double lastThroughputValue = (Double) lastThroughput.getResult();
            if (lastThroughputValue != null) {
               final double relDelta = Math.abs(currentThroughput / lastThroughputValue - 1.0);
               final double absDelta = Math.abs(currentThroughput - lastThroughputValue);
               if (log.isTraceEnabled()) {
                  log.trace("checkingPeriodIndex=" + checkingPeriodIndex + ", currentThroughput=" + currentThroughput + ", lastThroughput=" + lastThroughputValue + ", absDelta=" + absDelta + ", relDelta=" + relDelta);
               }
               if ((runInfo.getRunTime() > minimalWarmUpDuration) && (getMaxIteration() > minimalWarmUpCount) && (absDelta < absoluteThreshold || relDelta < relativeThreshold)) {
                  if (log.isInfoEnabled()) {
                     log.info("The tested system is warmed up.");
                  }
                  reportManager.reset();
                  runInfo.removeTag(PerfCakeConst.WARM_UP_TAG);
                  warmed = true;
               }
            }
            lastThroughput.add(currentThroughput);
         }
      }
   }

   @Override
   protected boolean checkStart() {
      return true;
   }

   /**
    * Gets the value of minimal warm-up period duration.
    *
    * @return The minimal warm-up period duration.
    */
   public long getMinimalWarmUpDuration() {
      return minimalWarmUpDuration;
   }

   /**
    * Sets the value of minimal warm-up period duration.
    *
    * @param minimalWarmUpDuration
    *       The minimal warm-up period duration to set.
    * @return Instance of this for fluent API.
    */
   public WarmUpReporter setMinimalWarmUpDuration(final long minimalWarmUpDuration) {
      this.minimalWarmUpDuration = minimalWarmUpDuration;
      return this;
   }

   /**
    * Gets the value of minimal warm-up iteration count.
    *
    * @return The value of minimal warm-up iteration count.
    */
   public long getMinimalWarmUpCount() {
      return minimalWarmUpCount;
   }

   /**
    * Sets the value of minimal warm-up iteration count.
    *
    * @param minimalWarmUpCount
    *       The value of minimal warm-up iteration count to set.
    * @return Instance of this for fluent API.
    */
   public WarmUpReporter setMinimalWarmUpCount(final long minimalWarmUpCount) {
      this.minimalWarmUpCount = minimalWarmUpCount;
      return this;
   }

   /**
    * Gets the value of relative threshold.
    *
    * @return The value of relative threshold.
    */
   public double getRelativeThreshold() {
      return relativeThreshold;
   }

   /**
    * Sets the value of relative threshold.
    *
    * @param relativeThreshold
    *       The value of relative threshold to set.
    * @return Instance of this for fluent API.
    */
   public WarmUpReporter setRelativeThreshold(final double relativeThreshold) {
      this.relativeThreshold = relativeThreshold;
      return this;
   }

   /**
    * Gets the value of absolute threshold.
    *
    * @return The value of absolute threshold.
    */
   public double getAbsoluteThreshold() {
      return absoluteThreshold;
   }

   /**
    * Sets the value of absolute threshold.
    *
    * @param absoluteThreshold
    *       The value of absolute threshold to set.
    * @return Instance of this for fluent API.
    */
   public WarmUpReporter setAbsoluteThreshold(final double absoluteThreshold) {
      this.absoluteThreshold = absoluteThreshold;
      return this;
   }
}
