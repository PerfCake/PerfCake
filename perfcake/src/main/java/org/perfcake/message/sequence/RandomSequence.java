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
package org.perfcake.message.sequence;

import org.perfcake.PerfCakeException;

import java.util.Random;

/**
 * Sequence of random numbers in the given range <min, max).
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RandomSequence implements Sequence {

   /**
    * Minimal number in the sequence (inclusive).
    */
   private int min = 0;

   /**
    * Maximal number in the sequence (exclusive).
    */
   private int max = 100;

   /**
    * Random number generator.
    */
   private Random rnd = new Random();

   @Override
   public String getNext() {
      return String.valueOf(rnd.nextInt(max - min) + min);
   }

   @Override
   public void reset() throws PerfCakeException {
      // we could create a new instance of random, but it does not make much sense
   }

   /**
    * Gets the minimal number in the sequence (inclusive).
    *
    * @return The minimal number in the sequence.
    */
   public int getMin() {
      return min;
   }

   /**
    * Sets the minimal number in the sequence (inclusive).
    *
    * @param min
    *       The minimal number in the sequence.
    */
   public void setMin(final int min) {
      this.min = min;
   }

   /**
    * Gets the maximal number in the sequence (exclusive).
    *
    * @return The maximal number in the sequence.
    */
   public int getMax() {
      return max;
   }

   /**
    * Sets the maximal number in the sequence (exclusive).
    *
    * @param max
    *       The maximal number in the sequence.
    */
   public void setMax(final int max) {
      this.max = max;
   }
}
