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
package org.perfcake.nreporting.reporters.accumulators;

/**
 * Accumulates an arithmetic average
 * 
 * TODO write a test in cooperation with ResponseTimeReporter
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class AvgAccumulator implements Accumulator<Double> {

   private long count = 0;
   private double sum;

   @Override
   public synchronized void add(final Double number) {
      count = count + 1;
      sum = sum + number;
   }

   @Override
   public Double getResult() {
      if (count == 0) {
         return 0d;
      } else {
         return sum / count;
      }
   }

}
