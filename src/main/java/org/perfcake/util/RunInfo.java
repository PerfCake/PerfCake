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
package org.perfcake.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.xnio.channels.UnsupportedOptionException;

/**
 * Information about the current scenario run.
 * TODO this should be directly referenced from Scenario, needs to be fixed after the new reporting is done, it should also go to a different package
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class RunInfo {

   /**
    * Unique identification string of the current measurement run
    * TODO use this in scenario, currently only null is passed in
    */
   private final String id;

   /**
    * How long is this measurement scheduled to run in milliseconds or iterations.
    * Another {@link org.perfcake.util.PeriodType Period Types} do not make any sense here.
    */
   private final Period duration;

   /**
    * Unix time of the measurement start. If the system clock changes during the run, the results based
    * on this value are influenced. The iterations however use {@link System.nanoTime()} so there is no
    * worry.
    */
   private long startTime = -1;

   /**
    * Unix time of the measurement end. If the system clock changes during the run, the results based
    * on this value are influenced. The iterations however use {@link System.nanoTime()} so there is no
    * worry.
    */
   private long endTime = -1;

   /**
    * Number of the last iteration.
    */
   private final AtomicLong iterations = new AtomicLong(0);

   /**
    * Tags associated with this measurement run.
    */
   private final Set<String> tags = new HashSet<>();

   /**
    * Creates a new RunInfo.
    * 
    * @param id
    *           Unique identifier of this run
    * @param duration
    *           Target duration of the run (time or iterations)
    */
   public RunInfo(final String id, final Period duration) {
      this.id = id;
      this.duration = duration;

      assert duration.getPeriodType() == PeriodType.ITERATION || duration.getPeriodType() == PeriodType.TIME : "Unsupported RunInfo duration set.";
   }

   /**
    * Starts a new measurement run. This resets the iteration counter.
    */
   public void start() {
      reset();
      startTime = System.currentTimeMillis();
   }

   /**
    * Stops the measurement run.
    */
   public void stop() {
      endTime = System.currentTimeMillis();
   }

   /**
    * Get the current iteration counter value.
    * This can be used as an approximate value of passed iteration even though some of them
    * might still be pending their execution.
    * 
    * @return Current iteration counter value
    */
   public long getIteration() {
      return iterations.get();
   }

   /**
    * Get the next iteration counter value.
    * This automatically increases the iteration counter for the next call to obtain a different value.
    * 
    * @return The next available iteration counter value
    */
   public long getNextIteration() {
      return iterations.getAndIncrement();
   }

   /**
    * Resets iteration counter.
    * Use with maximal caution!
    * 
    * TODO Make this a protected/private method
    */
   public void reset() {
      iterations.set(0);
   }

   /**
    * Gets the current measurement run time in millisecond. If the system clock changed
    * during the running measurement, this value will be influenced.
    * 
    * @return Current measurement run time
    */
   public long getRunTime() {
      if (startTime == -1) {
         return 0;
      } else if (endTime == -1) {
         return System.currentTimeMillis() - startTime;
      } else {
         return endTime - startTime;
      }
   }

   /**
    * Gets the current measurement progress in percents.
    * If the run is limited by time based period and the system clock changed during the measurement,
    * the result value will be influenced.
    * 
    * @return Completed percents of the current measurement
    */
   public double getPercentage() {
      if (!isRunning()) {
         return 0;
      }

      double progress;

      switch (duration.getPeriodType()) {
         case ITERATION:
            progress = getIteration();
            break;
         case TIME:
            progress = getRunTime();
            break;
         default:
            throw new UnsupportedOptionException("Detected unsupported ReportPeriod type.");
      }

      return (progress / duration.getPeriod()) * 100;
   }

   /**
    * Gets current measurement unique identifier. This can be null.
    * 
    * @return Measurement run identifier
    */
   public String getId() {
      return id;
   }

   /**
    * Gets Unix time of the measurement start. If the system clock changed during the measurement,
    * this value will not represent the real start.
    * 
    * @return Measurement start time
    */
   public long getStartTime() {
      return startTime;
   }

   /**
    * Gets Unix time of the measurement end.
    * 
    * @return Measurement end time
    */
   public long getEndTime() {
      return endTime;
   }

   /**
    * Is there a running measurement? This is true if and only if {@link #start()} has been called
    * and there has been no call to {@link #stop()} since then.
    * 
    * @return True if the measurement is running
    */
   public boolean isRunning() {
      return startTime != -1 && endTime == -1;
   }

   /**
    * Gets an unmodifiable set of tags associated with this measurement run.
    * 
    * @return An unmodifiable set of tags
    */
   public Set<String> getTags() {
      return Collections.unmodifiableSet(tags);
   }

   /**
    * Associate a new tag with this measurement.
    * 
    * @param tag
    *           A new tag to be associated
    */
   public void addTag(final String tag) {
      tags.add(tag);
   }

   /**
    * A set of tags to be associated with the current measurement.
    * 
    * @param tags
    *           A set of tags to be associated
    */
   public void addTags(final Set<String> tags) {
      this.tags.addAll(tags);
   }
}
