/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.perfcake.nreporting;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A result of the smallest measurement unit - an iteration.
 * One should obtain a new instance of a MeasuremenUnit using {@link org.perfcake.nreporting.ReportManager#newMeasurementUnit()}.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class MeasurementUnit implements Serializable {

   private static final long serialVersionUID = 3596375306594505085L;

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
   private long totalTime = 0;

   /**
    * Custom results reported by a sender.
    */
   private final Map<String, Object> measurementResults = new HashMap<>();

   /**
    * Constructor is protected. Use {@link org.perfcake.nreporting.ReportManager#newMeasurementUnit()} to obtain a new instance.
    * 
    * @param iteration
    *           Current iteration number.
    */
   protected MeasurementUnit(final long iteration) {
      this.iteration = iteration;
   }

   /**
    * Append a custom result.
    * 
    * @param label
    *           The label of the result.
    * @param value
    *           The value of the result.
    */
   public void appendResult(final String label, final Object value) {
      measurementResults.put(label, value);
   }

   /**
    * Get immutable map with all the custom results.
    * 
    * @return An immutable copy of the custom results map.
    */
   public Map<String, Object> getResults() {
      return Collections.unmodifiableMap(measurementResults);
   }

   /**
    * Get a custom result for the given label.
    * 
    * @param label
    *           The label of the custom result.
    * @return The value for the given custom result.
    */
   public Object getResult(final String label) {
      return measurementResults.get(label);
   }

   /**
    * Starts measuring. This is independent on current system time.
    */
   public void startMeasure() {
      startTime = System.nanoTime();
      stopTime = -1;
   }

   /**
    * Stopts measuring.
    */
   public void stopMeasure() {
      stopTime = System.nanoTime();
      totalTime = totalTime + getLastTime();
   }

   /**
    * Gets total time measured during all mesurements done by this Measurement Unit (all time periods between calls to {@link #startMeasure()} and {@link #stopMeasure()} in milliseconds.
    * 
    * @return The total time measured by this unit in milliseconds.
    */
   public long getTotalTime() {
      return totalTime;
   }

   /**
    * Gets time of the last measurement (time period between calls to {@link #startMeasure()} and {@link #stopMeasure()} in milliseconds.
    * 
    * @return Time of the last measurement in milliseconds.
    */
   public long getLastTime() {
      if (startTime == -1 || stopTime == -1) {
         return -1;
      }

      return (stopTime - startTime) / 1_000_000;
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
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (iteration ^ (iteration >>> 32));
      result = prime * result + ((measurementResults == null) ? 0 : measurementResults.hashCode());
      result = prime * result + (int) (startTime ^ (startTime >>> 32));
      result = prime * result + (int) (stopTime ^ (stopTime >>> 32));
      result = prime * result + (int) (totalTime ^ (totalTime >>> 32));
      return result;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      MeasurementUnit other = (MeasurementUnit) obj;
      if (iteration != other.iteration)
         return false;
      if (measurementResults == null) {
         if (other.measurementResults != null)
            return false;
      } else if (!measurementResults.equals(other.measurementResults))
         return false;
      if (startTime != other.startTime)
         return false;
      if (stopTime != other.stopTime)
         return false;
      if (totalTime != other.totalTime)
         return false;
      return true;
   }
}
