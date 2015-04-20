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
package org.perfcake;

import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Information about the current scenario run.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RunInfo {

   /**
    * How long is this measurement scheduled to run in milliseconds or iterations.
    * Another {@link org.perfcake.common.PeriodType Period Types} do not make any sense here.
    */
   private final Period duration;

   /**
    * Unix time of the measurement start. If the system clock changes during the run, the results based
    * on this value are influenced. The iterations however use {@link System#nanoTime()} so there is no
    * worry.
    */
   private long startTime = -1;

   /**
    * Unix time of the measurement end. If the system clock changes during the run, the results based
    * on this value are influenced. The iterations however use {@link System#nanoTime()} so there is no
    * worry.
    */
   private long endTime = -1;

   /**
    * Number of threads that is currently used to generate the load.
    */
   private volatile int threads = 1;

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
    * @param duration
    *       Target duration of the run (time or iterations)
    */
   public RunInfo(final Period duration) {
      this.duration = duration;

      assert duration.getPeriodType() == PeriodType.ITERATION || duration.getPeriodType() == PeriodType.TIME : "Unsupported RunInfo duration set.";
   }

   /**
    * Starts a new measurement run. This resets the iteration counter.
    */
   public void start() {
      startTime = System.currentTimeMillis();
      endTime = -1;

      iterations.set(0);
   }

   /**
    * Resets this RunInfo to its original state. If there is a running measurement, a new start time is obtained for the measurement
    * to be able to continue smoothly.
    */
   public void reset() {
      if (isRunning()) {
         startTime = System.currentTimeMillis();
      } else {
         startTime = -1;
      }
      endTime = -1;

      iterations.set(0);
   }

   /**
    * Stops the measurement run.
    */
   public void stop() {
      if (endTime == -1) {
         endTime = System.currentTimeMillis();
      }
   }

   /**
    * Gets the current iteration counter value.
    * This can be used as an approximate value of passed iterations even though some of them
    * might still be pending their execution.
    *
    * @return Current iteration counter value, -1 if there was no iteration so far.
    */
   public long getIteration() {
      return iterations.get() - 1; // actual number of iterations is 1 higher as we use getAndIncrement()
   }

   /**
    * Gets the next iteration counter value.
    * This automatically increases the iteration counter for the next call to obtain a different value.
    *
    * @return The next available iteration counter value.
    */
   public long getNextIteration() {
      return iterations.getAndIncrement();
   }

   /**
    * Gets the current measurement run time in millisecond. If the system clock changed
    * during the running measurement, this value will be influenced.
    *
    * @return Current measurement run time.
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
    * @return Completed percents of the current measurement.
    */
   public double getPercentage() {
      return getPercentage(getIteration());
   }

   /**
    * Gets the theoretical measurement progress in percents based on the provided number of passed iterations.
    * If the run is limited by time based period and the system clock changed during the measurement,
    * the result value will be influenced.
    *
    * @param iteration
    *       The iteration for which we want to know the progress % number compared to the configured period duration.
    * @return Completed percents of the theoretical measurement state. This cannot be more than 100% no matter what value is provided.
    */
   public double getPercentage(final long iteration) {
      if (startTime == -1) {
         return 0d;
      }

      double progress;

      switch (duration.getPeriodType()) {
         case ITERATION:
            progress = Math.min(iteration + 1, duration.getPeriod()); // at the beginning, iteration can be -1, and we do not want to report more than 100%
            break;
         case TIME:
            progress = Math.min(getRunTime(), duration.getPeriod());
            break;
         default:
            throw new IllegalArgumentException("Detected unsupported ReportPeriod type.");
      }

      return (progress / duration.getPeriod()) * 100;
   }

   /**
    * Gets Unix time of the measurement start. If the system clock changed during the measurement,
    * this value will not represent the real start.
    *
    * @return Measurement start time.
    */
   public long getStartTime() {
      return startTime;
   }

   /**
    * Gets Unix time of the measurement end.
    *
    * @return Measurement end time.
    */
   public long getEndTime() {
      return endTime;
   }

   /**
    * Is there a running measurement? This is true if and only if {@link #isStarted()} returns true and we did not reached
    * the final iterations.
    *
    * @return True if the measurement is running.
    */
   public boolean isRunning() {
      return isStarted() && !reachedLastIteration();
   }

   /**
    * Was the measurement started? This is true if and only if {@link #start()} has been called
    * and there has been no call to {@link #stop()} since then.
    *
    * @return True if the measurement has been started.
    */
   public boolean isStarted() {
      return startTime != -1 && endTime == -1;
   }

   /**
    * Returns true if the last iteration has been reached.
    *
    * @return True if the last iteration has been reached.
    */
   private boolean reachedLastIteration() {
      return (duration.getPeriodType().equals(PeriodType.ITERATION) && iterations.get() >= duration.getPeriod()) || (duration.getPeriodType().equals(PeriodType.TIME) && getRunTime() >= duration.getPeriod());
   }

   /**
    * Gets an unmodifiable set of tags associated with this measurement run.
    *
    * @return An unmodifiable set of tags.
    */
   public Set<String> getTags() {
      return Collections.unmodifiableSet(tags);
   }

   /**
    * Checks for a presence of a given tag.
    *
    * @param tag
    *       A tag to be checked.
    * @return <code>true</code> if the specified tag is set for this run info.
    */
   public boolean hasTag(final String tag) {
      return tags.contains(tag);
   }

   /**
    * Associates a new tag with this measurement.
    *
    * @param tag
    *       A new tag to be associated.
    */
   public void addTag(final String tag) {
      tags.add(tag);
   }

   /**
    * Adds a set of tags to be associated with the current measurement.
    *
    * @param tags
    *       A set of tags to be associated.
    */
   public void addTags(final Set<String> tags) {
      this.tags.addAll(tags);
   }

   /**
    * Removes a tag from this run.
    *
    * @param tag
    *       A tag to be removed.
    */
   public void removeTag(final String tag) {
      this.tags.remove(tag);
   }

   /**
    * Gets the desired run duration
    *
    * @return The run duration.
    */
   public Period getDuration() {
      return duration;
   }

   @Override
   public String toString() {
      return String.format("RunInfo [duration=%s, startTime=%d, endTime=%d, iterations=%d, tags=%s, started=%d, running=%d, percentage=%.3f]", duration, startTime, endTime, getIteration(), tags, isStarted() ? 1 : 0, isRunning() ? 1 : 0, getPercentage());
   }

   /**
    * Returns number of threads that is currently used to generate the load.
    *
    * @return The number of threads.
    */
   public int getThreads() {
      return threads;
   }

   /**
    * Sets the information about the number of threads that is currently used to generate the load.
    *
    * @param threads
    *       The number of threads.
    */
   public void setThreads(final int threads) {
      this.threads = threads;
   }
}
