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

/**
 * Fake measurement unit for test usage.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FakeMeasurementUnit extends MeasurementUnit {

   private double time = 0d;

   public FakeMeasurementUnit(final long iteration) {
      super(iteration);
   }

   public void setTime(final double time) {
      this.time = time;
   }

   @Override
   public double getTotalTime() {
      return time;
   }

   @Override
   public double getLastTime() {
      return time;
   }
}
