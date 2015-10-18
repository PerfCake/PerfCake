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
package org.perfcake.message.sequence;

import org.perfcake.PerfCakeException;

/**
 * Simple sequence of numbers.
 * Can go in both directions, can specify boundaries and a step.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class NumberSequence implements Sequence {

   /**
    * Beginning of the sequence.
    */
   private long start = 0;

   /**
    * Last number of the sequence.
    */
   private long end = Long.MIN_VALUE;

   /**
    * Step in the sequence.
    */
   private long step = 1;

   /**
    * Current sequence value;
    */
   private long value = 0;

   /**
    * Should we start from the beginning after reaching the last number in the sequence?
    */
   private boolean cycle = true;

   @Override
   public synchronized String getNext() {
      final long res = value;
      final boolean internalCycle = isCycle();

      long newValue = value + step;

      if (step > 0) {
         if (newValue < value && end > Long.MIN_VALUE) { // overflow
            if (!internalCycle) {
               newValue = Long.MAX_VALUE;
            }
         }

         value = newValue;

         if (end > Long.MIN_VALUE && value > end) {
            if (internalCycle) {
               value = start;
            } else {
               value = end;
            }
         }
      } else {
         if (newValue > value && end < Long.MAX_VALUE) { // underflow
            if (!internalCycle) {
               newValue = Long.MIN_VALUE;
            }
         }

         value = newValue;

         if (end < Long.MAX_VALUE && value < end) {
            if (internalCycle) {
               value = start;
            } else {
               value = end;
            }
         }

      }

      return String.valueOf(res);
   }

   @Override
   public synchronized void reset() throws PerfCakeException {
      value = getStart();
   }

   /**
    * Gets the beginning of the sequence.
    *
    * @return The beginning of the sequence.
    */
   public long getStart() {
      return start;
   }

   /**
    * Sets the beginning of the sequence.
    *
    * @param start
    *       The beginning of the sequence.
    */
   public void setStart(final long start) {
      this.start = start;
   }

   /**
    * Gets the last value in the sequence.
    *
    * @return The last value in the sequence.
    */
   public long getEnd() {
      return end;
   }

   /**
    * Sets the last value in the sequence.
    * Set to {@link Long#MIN_VALUE} in case of positive steps (or to {@link Long#MAX_VALUE} in case of negative steps) to disable check of the last value in the interval.
    *
    * @param end
    *       The last value in the sequence.
    */
   public void setEnd(final long end) {
      this.end = end;
   }

   /**
    * Gets the step size.
    *
    * @return The step size.
    */
   public long getStep() {
      return step;
   }

   /**
    * Sets the step size. If the check for the last value of the interval is disabled, this configuration is preserved even for step with negative numbers.
    *
    * @param step
    *       The step size.
    */
   public void setStep(final long step) {
      if (step < 0 && end == Long.MIN_VALUE) {
         end = Long.MAX_VALUE;
      } else if (step > 0 && end == Long.MAX_VALUE) {
         end = Long.MIN_VALUE;
      }

      this.step = step;
   }

   /**
    * Determines whether the sequence cycles around (starts from the beginning after reaching the last value).
    *
    * @return True if and only if the sequence cycles around.
    */
   public boolean isCycle() {
      return cycle;
   }

   /**
    * Specifies whether the sequence cycles around (starts from the beginning after reaching the last value).
    *
    * @param cycle
    *       True to allow the sequence to cycle around.
    */
   public void setCycle(final boolean cycle) {
      this.cycle = cycle;
   }
}
