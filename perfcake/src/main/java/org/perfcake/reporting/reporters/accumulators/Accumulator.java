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
package org.perfcake.reporting.reporters.accumulators;

/**
 * Accumulator is a tool for reporters to accumulate multiple values from measurement units into a single mesurement.
 * Accumulator must be thread safe as it might be called from multiple threads at the same time.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface Accumulator<T> {

   /**
    * Adds a value to the accumulator. This value is accumulated with the previously reported values.
    *
    * @param value
    *       The value to be accumulated.
    */
   public void add(T value);

   /**
    * Gets the accumulated result of currently added values.
    *
    * @return The result of the accumulation.
    */
   public T getResult();

   /**
    * Resets the accumulator to the default state.
    */
   public void reset();
}
