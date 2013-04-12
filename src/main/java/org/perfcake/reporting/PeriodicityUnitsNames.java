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

package org.perfcake.reporting;

/**
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class PeriodicityUnitsNames {
   public static String ITERATIONS_NAME = "it";

   public static String PERCENTS_NAME = "%";

   public static String SECONDS_NAME = "s";

   public static String MINUTES_NAME = "m";

   public static String HOURS_NAME = "h";

   public static String DAYS_NAME = "d";

   public static String[] allNames() {
      return new String[] { ITERATIONS_NAME, PERCENTS_NAME, SECONDS_NAME, MINUTES_NAME, DAYS_NAME };
   }

   public static PeriodicityUnit getUnitByName(String unitName) {
      if (unitName.equals(ITERATIONS_NAME)) {
         return PeriodicityUnit.Iteration;
      }

      if (unitName.equals(PERCENTS_NAME)) {
         return PeriodicityUnit.Percent;
      }

      if (unitName.equals(SECONDS_NAME)) {
         return PeriodicityUnit.Seconds;
      }

      if (unitName.equals(MINUTES_NAME)) {
         return PeriodicityUnit.Minutes;
      }

      if (unitName.equals(HOURS_NAME)) {
         return PeriodicityUnit.Hours;
      }

      if (unitName.equals(DAYS_NAME)) {
         return PeriodicityUnit.Days;
      }

      return null;
   }
}
