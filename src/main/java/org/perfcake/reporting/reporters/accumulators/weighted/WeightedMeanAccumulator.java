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
package org.perfcake.reporting.reporters.accumulators.weighted;

import org.perfcake.reporting.reporters.accumulators.Accumulator;

/**
 * Accumulates weighted mean.
 * For values x1, x2, x3... and their respective weights w1, w2, w3... the result is
 * (x1 * w1 + x2 * w2 + x3 * w3 + ...) / (w1 + w2 + w3 + ...).
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class WeightedMeanAccumulator implements Accumulator<WeightedValue<Double>> {

   private double sum;
   private long weight;

   @Override
   public synchronized void add(final WeightedValue<Double> value) {
      sum = sum + (value.getValue() * value.getWeight());
      weight = weight + value.getWeight();
   }

   /**
    * Returns the weighted mean of all accumulated values. When there are no accumulated values, the result is
    * Double.NaN. The weight of the result is always 1.
    *
    * @return The weighted mean of all accumulated values.
    */
   @Override
   public synchronized WeightedValue<Double> getResult() {
      return new WeightedValue<>(sum / weight, 1);
   }

   /**
    * Resets the accumulator to its default state, please note that this results to the accumulated value of Double.NaN
    * as there is nothing accumulated and no mean can be computed.
    */
   @Override
   public synchronized void reset() {
      sum = 0d;
      weight = 0l;
   }
}
