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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.perfcake.RunInfo;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;

/**
 * Basic reporter that should be used to write any real reporter. This implementation makes sure
 * that the contract defined as part of {@link Reporter} is held. The class is also well tested.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public abstract class AbstractReporter implements Reporter {

   private static final Logger log = Logger.getLogger(AbstractReporter.class);

   /**
    * Remembers the last observed percentage state of the measurement run. This is used to report change to this value only once.
    */
   private long lastPercentage = -1;

   /**
    * RunInfo associated with current measurement.
    */
   protected RunInfo runInfo = null;

   /**
    * Thread to assure time based periodical reporting.
    */
   private Thread periodicThread;

   /**
    * Set of periods bound to destinations. This is used to register destinations and requested reporting periods.
    */
   private final Set<BoundPeriod<Destination>> periods = new HashSet<>();

   /**
    * Accumulators to accumulate results from multiple {@link org.perfcake.reporting.MeasurementUnit Measurement Units}.
    */
   @SuppressWarnings("rawtypes")
   private Map<String, Accumulator> accumulatedResults = new HashMap<>();

   /**
    * Reports a single {@link org.perfcake.reporting.MeasurementUnit} to this reporter. This calls {@link #doReport(MeasurementUnit)} overrided by a child,
    * accumulates results and reports iteration change and percentage change (if any).
    */
   @Override
   public final void report(final MeasurementUnit mu) throws ReportingException {
      if (runInfo == null) {
         throw new ReportingException("RunInfo has not been set for this reporter.");
      }

      doReport(mu);

      accumulateResults(mu.getResults());

      reportIterations(mu.getIteration());

      // report each percentage value just once
      final long percentage = (long) Math.floor(runInfo.getPercentage());
      if (percentage != lastPercentage) {
         lastPercentage = percentage;
         reportPercentage(percentage);
      }
   }

   /**
    * Gets a new measurement pre-filled with values from current run info.
    * 
    * @return The new measurement with current values from run info.
    */
   public Measurement newMeasurement() {
      return new Measurement(Math.round(runInfo.getPercentage()), runInfo.getRunTime(), runInfo.getIteration());
   }

   /**
    * Gets a value of an accumulated result.
    * 
    * @param key
    *           Key in the results hash map.
    * @return The value associated with the given key.
    */
   protected Object getAccumulatedResult(final String key) {
      return accumulatedResults.get(key) == null ? null : accumulatedResults.get(key).getResult();
   }

   /**
    * Copies current accumulated results to the provided {@link org.perfcake.reporting.Measurement}. This can be used in the child's {@link #doPublishResult(PeriodType, Destination)} method.
    * 
    * @param m
    *           The {@link org.perfcake.reporting.Measurement} to be filled with the results.
    */
   protected void publishAccumulatedResult(final Measurement m) {
      for (final String key : accumulatedResults.keySet()) {
         m.set(key, accumulatedResults.get(key).getResult());
      }
   }

   /**
    * For each key of the Mesurement Unit's results map, ask for an accumulator and accumulate the value with the previous values.
    * Childs can use this method to accumulate the main result as well (be it a total response time or anything else).
    * 
    * @param results
    *           The hash map with results to be accumulated.
    */
   @SuppressWarnings({ "unchecked", "rawtypes" })
   protected void accumulateResults(final Map<String, Object> results) {
      for (final Entry<String, Object> entry : results.entrySet()) {
         // make sure we have an accumulator set to be able to accumulate the result
         if (accumulatedResults.get(entry.getKey()) == null) {
            final Accumulator a = getAccumulator(entry.getKey(), entry.getValue().getClass());

            if (a == null) {
               log.warn(String.format("No accumulator specified for results key '%s' and its type '%s'.", entry.getKey(), entry.getValue().getClass().getCanonicalName()));
            } else {
               accumulatedResults.put(entry.getKey(), a);
               accumulatedResults.get(entry.getKey()).add(entry.getValue());
            }
         } else {
            accumulatedResults.get(entry.getKey()).add(entry.getValue());
         }
      }
   }

   /**
    * Gets an appropriate accumulator for a given key from the Measuremen Unit's results map and its class.
    * This must be specified by the child classes.
    * 
    * @param key
    *           Name of the key from the results map.
    * @param clazz
    *           Class of the object in the results map.
    * @return An appropriate accumulator instance.
    */
   @SuppressWarnings("rawtypes")
   abstract protected Accumulator getAccumulator(String key, Class clazz);

   /**
    * Reports iteration changes to registered destinations.
    * 
    * @param iteration
    *           Iteration number.
    * @throws ReportingException
    */
   private void reportIterations(final long iteration) throws ReportingException {
      for (final BoundPeriod<Destination> bp : periods) {
         if (bp.getPeriodType() == PeriodType.ITERATION && (iteration == 0 || (iteration + 1) % bp.getPeriod() == 0)) {
            doPublishResult(PeriodType.ITERATION, bp.getBinding());
         }
      }
   }

   /**
    * Reports percentage changes to registered destinations.
    * 
    * @param percentage
    *           Percentage status of the run.
    * @throws ReportingException
    */
   private void reportPercentage(final long percentage) throws ReportingException {
      for (final BoundPeriod<Destination> bp : periods) {
         if (bp.getPeriodType() == PeriodType.PERCENTAGE && percentage % bp.getPeriod() == 0) {
            doPublishResult(PeriodType.PERCENTAGE, bp.getBinding());
         }
      }
   }

   @Override
   public final void registerDestination(final Destination d, final Period p) {
      if (p.getPeriodType() == PeriodType.TIME && p.getPeriod() < 500) {
         log.error("Periodical reporting with time period smaller than 500ms! Ignoring this reporting configuration.");
      } else {
         periods.add(new BoundPeriod<>(p, d));
      }
   }

   @Override
   public final void registerDestination(final Destination d, final Set<Period> periods) {
      for (final Period p : periods) {
         registerDestination(d, p);
      }
   }

   @Override
   public final void unregisterDestination(final Destination d) {
      final Set<BoundPeriod<Destination>> toBeRemoved = new HashSet<>();
      for (final BoundPeriod<Destination> bp : periods) {
         if (bp.getBinding().equals(d)) {
            toBeRemoved.add(bp);
         }
      }

      periods.removeAll(toBeRemoved);

      // close destinations (only once) if the measurement is running
      final Set<Destination> closed = new HashSet<>();
      if (runInfo.isRunning()) {
         for (final BoundPeriod<Destination> bp : toBeRemoved) {
            if (!closed.contains(bp.getBinding())) {
               bp.getBinding().close();
               closed.add(bp.getBinding());
            }
         }
      }
   }

   @Override
   public final void reset() {
      lastPercentage = -1;
      accumulatedResults = new HashMap<>();
      doReset();
   }

   /**
    * Reset reporter to a default state. All results should be forgotten.
    */
   abstract protected void doReset();

   /**
    * Process a new {@link org.perfcake.reporting.MeasurementUnit}.
    * 
    * @param mu
    *           A {@link org.perfcake.reporting.MeasurementUnit} to be processed.
    * @throws ReportingException
    */
   abstract protected void doReport(MeasurementUnit mu) throws ReportingException;

   /**
    * Publish results to the destination. This method is called only when the results should be published.
    * 
    * @param periodType
    *           A period type that caused the invocation of this method.
    * @param d
    *           A destination to which the result should be reported.
    * @throws ReportingException
    */
   abstract protected void doPublishResult(PeriodType periodType, Destination d) throws ReportingException;

   @Override
   public final Set<Destination> getDestinations() {
      final Set<Destination> result = new HashSet<>();
      for (final BoundPeriod<Destination> bp : periods) {
         result.add(bp.getBinding());
      }

      return Collections.unmodifiableSet(result);
   }

   @Override
   public void start() {
      assert runInfo != null : "RunInfo must be set prior to starting a reporter.";

      if (periods.size() == 0) {
         log.warn("No reporting periods are configured for this reporter (" + getClass().getCanonicalName() + "). The reporter is disabled. Call start() again after the periods are registered.");
         return;
      }

      reset();

      for (final Destination d : getDestinations()) {
         d.open();
      }

      periodicThread = new Thread(new Runnable() {
         @Override
         public void run() {
            long now;
            Long lastTime;
            Destination d;
            Map<Destination, Long> lastTimes = new HashMap<>();

            try {
               while (runInfo.isRunning() && !periodicThread.isInterrupted()) {
                  now = System.currentTimeMillis();

                  for (final BoundPeriod<Destination> p : periods) {
                     d = p.getBinding();
                     lastTime = lastTimes.get(d);

                     if (lastTime == null) {
                        lastTime = now;
                        lastTimes.put(d, lastTime);
                     }

                     if (p.getPeriodType() == PeriodType.TIME && lastTime + p.getPeriod() < now && runInfo.getIteration() >= 0) {
                        lastTimes.put(d, now);
                        try {
                           doPublishResult(PeriodType.TIME, d);
                        } catch (final ReportingException e) {
                           log.warn("Unable to publish result: ", e);
                        }
                     }
                  }

                  Thread.sleep(500);
               }
            } catch (final InterruptedException e) {
               // this means our job is done
            }
            if (log.isDebugEnabled()) {
               log.debug("Gratefully terminating the periodic reporting thread.");
            }
         }
      });
      periodicThread.setDaemon(true); // allow the thread to die with JVM termination and do not block it
      periodicThread.start();
   }

   @Override
   public void stop() {
      if (periodicThread != null) {
         periodicThread.interrupt();
      }
      periodicThread = null;

      for (final Destination d : getDestinations()) {
         d.close();
      }
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
