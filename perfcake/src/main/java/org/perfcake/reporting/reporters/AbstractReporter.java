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
import org.perfcake.RunInfo;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;
import org.perfcake.reporting.reporters.accumulators.MaxLongValueAccumulator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a basic reporter that makes sure that the contract defined as part of {@link Reporter} is held.
 * The class is also well tested.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public abstract class AbstractReporter implements Reporter {

   /**
    * The reporter's logger.
    */
   private static final Logger log = LogManager.getLogger(AbstractReporter.class);

   /**
    * Set of periods bound to destinations. This is used to register destinations and requested reporting periods.
    */
   private final Set<BoundPeriod<Destination>> periods = new HashSet<>();

   /**
    * ReportManager that owns this reporter.
    */
   protected ReportManager reportManager = null;

   /**
    * RunInfo associated with current measurement.
    */
   protected RunInfo runInfo = null;

   /**
    * Remembers the maximal value of observed MeasurementUnits.
    */
   private final MaxLongValueAccumulator maxIteration = new MaxLongValueAccumulator();

   /**
    * Remembers the last observed percentage state of the measurement run. This is used to report change to this value only once.
    */
   private long lastPercentage = -1L;

   /**
    * Accumulators to accumulate results from multiple {@link org.perfcake.reporting.MeasurementUnit Measurement Units}.
    */
   @SuppressWarnings("rawtypes")
   private Map<String, Accumulator> accumulatedResults = new ConcurrentHashMap<>();

   /**
    * Reports a single {@link org.perfcake.reporting.MeasurementUnit} to this reporter. This calls {@link #doReport(MeasurementUnit)} overridden by a child, accumulates results and reports iteration change and percentage change (if any).
    */
   @Override
   public final void report(final MeasurementUnit measurementUnit) throws ReportingException {
      if (runInfo == null) {
         throw new ReportingException("RunInfo has not been set for this reporter.");
      }

      reportIterationNumber(measurementUnit.getIteration(), measurementUnit);

      doReport(measurementUnit);

      accumulateResults(measurementUnit.getResults());

      reportIterations(measurementUnit.getIteration());

      reportAllPercentage((long) Math.floor(runInfo.getPercentage(measurementUnit.getIteration())));
   }

   private void reportAllPercentage(final long percentage) throws ReportingException {
      // report each percentage value just once
      if (percentage > lastPercentage) {
         while (percentage > lastPercentage + 1) { // we do not want to skip any percentage between prev. reporting and now
            lastPercentage = lastPercentage + 1;
            reportPercentage(lastPercentage);
         }
         lastPercentage = percentage; // simply cover the last case (percentage = lastPercentage + 1), and or the case when percentage got lower after a reset
         reportPercentage(percentage);
      }
   }

   protected Long getMaxIteration() {
      return maxIteration.getResult();
   }

   private void reportIterationNumber(final long iteration, final MeasurementUnit mu) {
      if (mu.startedAfter(runInfo.getStartTime())) { // only MUs from the current run should be taken into account
         maxIteration.add(iteration);
      }
   }

   /**
    * Gets a new measurement pre-filled with values from current run info.
    *
    * @return The new measurement with current values from run info.
    */
   public Measurement newMeasurement() {
      final Long iterations = maxIteration.getResult();
      final Measurement measurement = new Measurement(Math.round(runInfo.getPercentage(iterations)), runInfo.getRunTime(), iterations);
      measurement.set(PerfCakeConst.WARM_UP_TAG, runInfo.hasTag(PerfCakeConst.WARM_UP_TAG));
      measurement.set(PerfCakeConst.THREADS_TAG, runInfo.getThreads());
      return measurement;
   }

   /**
    * Gets a value of an accumulated result.
    *
    * @param key
    *       Key in the results hash map.
    * @return The value associated with the given key.
    */
   protected Object getAccumulatedResult(final String key) {
      final Accumulator accumulator = accumulatedResults.get(key);
      return accumulator == null ? null : accumulator.getResult();
   }

   @Override
   public void setReportManager(final ReportManager reportManager) {
      this.reportManager = reportManager;
   }

   /**
    * Copies current accumulated results to the provided {@link org.perfcake.reporting.Measurement}. This can be used in the child's {@link #publishResult(PeriodType, Destination)} method.
    *
    * @param measurement
    *       The {@link org.perfcake.reporting.Measurement} to be filled with the results.
    */
   protected void publishAccumulatedResult(final Measurement measurement) {
      for (final Entry<String, Accumulator> entry : accumulatedResults.entrySet()) {
         measurement.set(entry.getKey(), entry.getValue().getResult());
      }
   }

   /**
    * For each key of the Measurement Unit's results map, ask for an accumulator and accumulate the value with the previous values. Childs can use this method to accumulate the main result as well (be it a total response time or anything else).
    *
    * @param results
    *       The hash map with results to be accumulated.
    */
   @SuppressWarnings({ "unchecked", "rawtypes" })
   private void accumulateResults(final Map<String, Object> results) {
      for (final Entry<String, Object> entry : results.entrySet()) {
         final String key = entry.getKey();
         final Object value = entry.getValue();
         Accumulator accumulator = accumulatedResults.get(key); //IfAbsentPutWith(key, getAccumulatorFunction, entry);

         if (accumulator == null) {
            accumulator = getAccumulator(key, value.getClass());
            if (accumulator != null) {
               accumulator.add(value);
               accumulatedResults.put(key, accumulator);
            }
         } else {
            accumulator.add(value);
         }
      }
   }

   /**
    * Gets an appropriate accumulator for a given key from the Measurement Unit's results map and its class.
    * This should be overridden by the child classes. By default, last value accumulator is returned.
    * This must remain at least for {@link org.perfcake.PerfCakeConst#WARM_UP_TAG} and {@link org.perfcake.PerfCakeConst#THREADS_TAG}.
    *
    * @param key
    *       Name of the key from the results map.
    * @param clazz
    *       Class of the object in the results map.
    * @return An appropriate accumulator instance.
    */
   @SuppressWarnings("rawtypes")
   protected Accumulator getAccumulator(final String key, final Class clazz) {
      return new LastValueAccumulator();
   }

   /**
    * Reports iteration changes to registered destinations.
    *
    * @param iteration
    *       Iteration number.
    * @throws ReportingException
    *       Propagated from {@link #publishResult(org.perfcake.common.PeriodType, org.perfcake.reporting.destinations.Destination)}.
    */
   private void reportIterations(final long iteration) throws ReportingException {
      for (final BoundPeriod<Destination> boundPeriod : periods) {
         if (boundPeriod.getPeriodType() == PeriodType.ITERATION) {
            if ((iteration == 0 || (iteration + 1) % boundPeriod.getPeriod() == 0) || isLastIteration(iteration)) {
               publishResult(PeriodType.ITERATION, boundPeriod.getBinding());
            }
         }
      }
   }

   private boolean isLastIteration(final long iteration) {
      return runInfo.getDuration().getPeriodType().equals(PeriodType.ITERATION) && runInfo.getDuration().getPeriod() == iteration + 1;
   }

   /**
    * Reports percentage changes to registered destinations.
    *
    * @param percentage
    *       Percentage status of the run.
    * @throws ReportingException
    *       Propagated from {@link #publishResult(org.perfcake.common.PeriodType, org.perfcake.reporting.destinations.Destination)}.
    */
   private void reportPercentage(final long percentage) throws ReportingException {
      for (final BoundPeriod<Destination> boundPeriod : periods) {
         if (boundPeriod.getPeriodType() == PeriodType.PERCENTAGE) {
            long period = boundPeriod.getPeriod();
            if (((percentage != 0 || period <= 50) && (percentage % period == 0)) || percentage == 100) {
               publishResult(PeriodType.PERCENTAGE, boundPeriod.getBinding());
            }
         }
      }
   }

   @Override
   public final void registerDestination(final Destination destination, final Period period) {
      if (period.getPeriodType() == PeriodType.TIME && period.getPeriod() < 500) {
         log.error("Periodical reporting with time period smaller than 500ms! Ignoring this reporting configuration.");
      } else {
         periods.add(new BoundPeriod<>(period, destination));
      }
   }

   @Override
   public final void registerDestination(final Destination destination, final Set<Period> periods) {
      for (final Period period : periods) {
         registerDestination(destination, period);
      }
   }

   @Override
   public final void unregisterDestination(final Destination destination) {
      final Set<BoundPeriod<Destination>> toBeRemoved = periods.stream().filter(boundPeriod -> boundPeriod.getBinding().equals(destination)).collect(Collectors.toSet());

      periods.removeAll(toBeRemoved);

      // close destinations (only once) if the measurement is running
      final Set<Destination> closed = new HashSet<>();
      if (runInfo.isRunning()) {
         toBeRemoved.stream().filter(bp -> !closed.contains(bp.getBinding())).forEach(bp -> {
            bp.getBinding().close();
            closed.add(bp.getBinding());
         });
      }
   }

   @Override
   public final void reset() {
      lastPercentage = -1;
      maxIteration.reset();
      maxIteration.add(0l);
      accumulatedResults = new ConcurrentHashMap<>();
      doReset();
   }

   /**
    * Reset reporter to a default state. All results should be forgotten.
    */
   abstract protected void doReset();

   /**
    * Processes a new {@link org.perfcake.reporting.MeasurementUnit}.
    *
    * @param measurementUnit
    *       A {@link org.perfcake.reporting.MeasurementUnit} to be processed.
    * @throws ReportingException
    *       When it was not possible to process given {@link org.perfcake.reporting.MeasurementUnit}.
    */
   abstract protected void doReport(MeasurementUnit measurementUnit) throws ReportingException;

   @Override
   public final Set<Destination> getDestinations() {
      final Set<Destination> result = periods.stream().map(BoundPeriod::getBinding).collect(Collectors.toSet());

      return Collections.unmodifiableSet(result);
   }

   @Override
   public void start() {
      assert runInfo != null : "RunInfo must be set prior to starting a reporter.";

      if (checkStart()) {

         reset();

         getDestinations().forEach(org.perfcake.reporting.destinations.Destination::open);
      }
   }

   /**
    * Checks if the reporter is ready to be started.
    *
    * @return <code>true</code> if the reporter is ready to be started.
    */
   protected boolean checkStart() {
      if (periods.size() == 0) {
         log.warn("No reporting periods are configured for this reporter (" + getClass().getCanonicalName() + "). The reporter won't be able to output any results while it still processes the results and thus consuming precious time. Register periods and call start() again.");
         return false;
      }

      return true;
   }

   @Override
   public void stop() {
      getDestinations().forEach(org.perfcake.reporting.destinations.Destination::close);
   }

   @Override
   public final void setRunInfo(final RunInfo runInfo) {
      this.runInfo = runInfo;
   }

   @Override
   public final Set<BoundPeriod<Destination>> getReportingPeriods() {
      return Collections.unmodifiableSet(periods);
   }
}
