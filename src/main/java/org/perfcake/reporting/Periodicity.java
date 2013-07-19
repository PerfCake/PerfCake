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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * <p>
 * Periodicity says how often something should happen. Periodicity can be expressed in percents in which case the information about whole planned length of test case is required (in time or in iteration count). The most typical use case is time periodicity, where Periodicity is expressed in seconds, minutes or hours.
 * </p>
 * 
 */
public class Periodicity {
   private static final Logger log = Logger.getLogger(Periodicity.class);

   private TestRunInfo testRunInfo;

   private float value;

   private PeriodicityUnit unit;

   public Periodicity() {
   }

   public Periodicity(float value, PeriodicityUnit unit) {
      this.value = value;
      this.unit = unit;

   }

   public void setTestRunInfo(TestRunInfo tri) {
      this.testRunInfo = tri;

   }

   /**
    * 
    * @return True if and only if periodicity is time based.
    * @throws ReportsException
    */
   public boolean isTimely() throws ReportsException {
      checkPercentage();
      return unit == PeriodicityUnit.Seconds || unit == PeriodicityUnit.Minutes || unit == PeriodicityUnit.Hours || unit == PeriodicityUnit.Days;
   }

   public boolean isIterationary() throws ReportsException {
      checkPercentage();
      return unit == PeriodicityUnit.Iteration;
   }

   /**
    * This method will check whether periodicity is percentage. if so it will
    * have to be converted into timely or iterationary so it has to have access
    * to either fixed amount of iteration this test consists of or to fixed
    * time.
    * 
    * @throws ReportsException
    */
   private void checkPercentage() throws ReportsException {
      if (unit == PeriodicityUnit.Percent) {
         if (testRunInfo.getTestDuration() != -1) {
            value = (testRunInfo.getTestDuration()) * (value / 100);
            unit = PeriodicityUnit.Seconds;
            log.info("The periodicity is percentage and test duration [" + testRunInfo.getTestDuration() + "] is set. Converting periodicity to timely. [" + value + " seconds]");
         } else if (testRunInfo.getTestIterations() != -1) {
            value = (testRunInfo.getTestIterations()) * (value / 100);
            unit = PeriodicityUnit.Iteration;
            log.info("The periodicity is percentage and test iteration count is set. Converting periodicity to iteratinoary. [" + value + " iterations]");
         } else {
            throw new ReportsException("Test periodicity is set to percentage but the test run doesn't have fixed amount of iterations or fixed duration!");
         }
      }
   }

   /**
    * Periodicity in iterations. How often (after how many iterations) should
    * the reporting happen.
    * 
    * @return
    * @throws ReportsException
    */
   public int getIterationalPeriodicity() throws ReportsException {
      if (isIterationary()) {
         return Math.round(value);
      }

      throw new ReportsException("Periodicity is not iterationary but query for iterationary periodicity made!");
   }

   /**
    * What is timely periodicity in seconds. How often shoould action happen in
    * seconds.
    * 
    * @return
    * @throws ReportsException
    */
   public float getTimePeriodicity() throws ReportsException {
      if (isTimely()) {
         if (unit == PeriodicityUnit.Minutes) {
            value = value * 60;
         }
         if (unit == PeriodicityUnit.Hours) {
            value = value * 60 * 60;
         }
         if (unit == PeriodicityUnit.Days) {
            value = value * 60 * 60 * 24;
         }

         unit = PeriodicityUnit.Seconds;

         return value;
      }

      throw new ReportsException("Periodicity is not timely but query for time periodicity made!");
   }

   /**
    * 
    * @param textValue
    * @return
    * @throws ReportsException
    */
   public static Periodicity constructFromString(String textValue) throws ReportsException {
      /**
       * This pattern matches for example: -434.4 cm 11 m on the other hand this
       * pattern rejects: - 343 cm // the space after minus sign
       * 
       */
      Matcher matcher = null;
      for (String uname : PeriodicityUnitsNames.allNames()) {
         Pattern p = Pattern.compile("(-?\\d+?(.\\d+?)?)[ ]*" + uname);
         matcher = p.matcher(textValue);

         if (matcher.matches() && matcher.groupCount() == 2) {
            float result = Float.parseFloat(matcher.group(1).trim());
            Periodicity periodicity = new Periodicity(result, PeriodicityUnitsNames.getUnitByName(uname));
            log.info("Created periodicity from string [" + textValue + "]. Resulting periodicity [" + periodicity + "]");
            return periodicity;
         }
      }

      StringBuilder sb = new StringBuilder();
      for (String name : PeriodicityUnitsNames.allNames()) {
         sb.append(name);
         sb.append(" ");
      }
      throw new ReportsException("String [" + textValue + "] isn't valid periodicity. Valid format is <float number><unit> where unit is one of these: " + sb.toString());
   }

   @Override
   public String toString() {
      return "Periodicity{" + "testRunInfo=" + testRunInfo + ", value=" + value + ", unit=" + unit + '}';
   }
}
