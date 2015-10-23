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
package org.perfcake.reporting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A result of the smallest measurement unit - an iteration.
 * One should obtain a new instance of a MeasurementUnit using {@link org.perfcake.reporting.ReportManager#newMeasurementUnit()}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MeasurementUnit implements Serializable {

   private static final long serialVersionUID = 3596375306594505085L;

   /**
    * Logger.
    */
   private static final Logger log = LogManager.getLogger(MeasurementUnit.class);

   /**
    * Iteration for which this unit was created.
    */
   private final long iteration;

   /**
    * Time when last measurement started. A unit may accumulate more measurements together.
    */
   private long startTime = -1;

   /**
    * Time when last measurement ended.
    */
   private long stopTime = -1;

   /**
    * Total measured time.
    */
   private double totalTime = 0;

   /**
    * Custom results reported by a sender.
    */
   private final Map<String, Object> measurementResults = new HashMap<>();

   /**
    * When the measurement was first started in real time (timestamp value from {@link System#currentTimeMillis()}).
    */
   private long timeStarted = -1;

   /**
    * Constructor is protected. Use {@link org.perfcake.reporting.ReportManager#newMeasurementUnit()} to obtain a new instance.
    *
    * @param iteration
    *       Current iteration number.
    */
   protected MeasurementUnit(final long iteration) {
      this.iteration = iteration;
   }

   /**
    * Appends a custom result.
    *
    * @param label
    *       The label of the result.
    * @param value
    *       The value of the result.
    */
   public void appendResult(final String label, final Object value) {
      measurementResults.put(label, value);
   }

   /**
    * Gets immutable map with all the custom results.
    *
    * @return An immutable copy of the custom results map.
    */
   public Map<String, Object> getResults() {
      return Collections.unmodifiableMap(measurementResults);
   }

   /**
    * Gets a custom result for the given label.
    *
    * @param label
    *       The label of the custom result.
    * @return The value for the given custom result.
    */
   public Object getResult(final String label) {
      return measurementResults.get(label);
   }

   /**
    * Starts measuring. This is independent on current system time.
    */
   public void startMeasure() {
      timeStarted = System.currentTimeMillis();
      startTime = System.nanoTime();
      stopTime = -1;
   }

   /**
    * Stops measuring.
    */
   public void stopMeasure() {
      stopTime = System.nanoTime();
      totalTime = totalTime + getLastTime();
   }

   /**
    * Gets total time measured during all measurements done by this Measurement Unit (all time periods between calls to {@link #startMeasure()} and {@link #stopMeasure()} in milliseconds.
    *
    * @return The total time measured by this unit in milliseconds.
    */
   public double getTotalTime() {
      return totalTime;
   }

   /**
    * Gets time of the last measurement (time period between calls to {@link #startMeasure()} and {@link #stopMeasure()} in milliseconds.
    *
    * @return Time of the last measurement in milliseconds.
    */
   public double getLastTime() {
      if (startTime == -1 || stopTime == -1) {
         return -1;
      }

      if (stopTime - startTime == 0) {
         log.warn("Zero time measured! PerfCake is probably running on a machine where the internal timer does not provide enough resolution (e.g. a virtual machine). " +
               "Please refer to the Troubleshooting section in the User Guide.\nCurrent measurement unit: " + this.toString());
      }

      return (stopTime - startTime) / 1_000_000.0;
   }

   /**
    * Checks whether this measurement unit was first started after the specified time (Unix time in millis).
    *
    * @param ref
    *       The reference time to compare to the start of the measurement.
    * @return <code>true</code> if this measurement unit was first started after the specified reference time.
    */
   public boolean startedAfter(final long ref) {
      return timeStarted > ref;
   }

   /**
    * Gets the number of current iteration of this Measurement Unit.
    *
    * @return The number of iteration.
    */
   public long getIteration() {
      return iteration;
   }

   @Override
   public int hashCode() {
      int result;
      long temp;
      result = (int) (iteration ^ (iteration >>> 32));
      result = 31 * result + (int) (startTime ^ (startTime >>> 32));
      result = 31 * result + (int) (stopTime ^ (stopTime >>> 32));
      temp = Double.doubleToLongBits(totalTime);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      result = 31 * result + measurementResults.hashCode();
      result = 31 * result + (int) (timeStarted ^ (timeStarted >>> 32));
      return result;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }

      final MeasurementUnit that = (MeasurementUnit) obj;

      if (iteration != that.iteration) {
         return false;
      }
      if (startTime != that.startTime) {
         return false;
      }
      if (stopTime != that.stopTime) {
         return false;
      }
      if (timeStarted != that.timeStarted) {
         return false;
      }
      if (Double.compare(that.totalTime, totalTime) != 0) {
         return false;
      }
      if (!measurementResults.equals(that.measurementResults)) {
         return false;
      }

      return true;
   }

   @Override
   public String toString() {
      return "MeasurementUnit [" +
            "iteration=" + iteration +
            ", startTime=" + startTime +
            ", stopTime=" + stopTime +
            ", totalTime=" + totalTime +
            ", measurementResults=" + measurementResults +
            ", timeStarted=" + timeStarted +
            ']';
   }
}
