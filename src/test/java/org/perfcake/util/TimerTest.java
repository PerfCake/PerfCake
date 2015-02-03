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
package org.perfcake.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.io.Serializable;

/**
 * Testing of Timer extracted from Faban. This is a working version and will be incorporated properly, if at all.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class TimerTest {
   public static final Logger logger = LogManager.getLogger(TimerTest.class);
   public static final long TO_NANOS = 1000000L;

   /**
    * Timer for all benchmark runs.
    */
   public static class Timer implements Serializable {

      /**
       * The millisec epoch time of this benchmark.
       */
      long epochMillis;

      /**
       * The nanosec epoch time of this benchmark.
       */
      transient long epochNanos; // This has no meaning on a different system.

      transient long diffms; // The epoch difference, millisec part
      transient int diffns; // The epoch difference, nanosec part

      private long compensation = 5000000l;  // Some pretty good starting numbers
      private double deviation = 5000000d; // for both fields.
      private Boolean debug = null;

      /**
       * Default Constructor which saves the current time
       * as epochMillis and epochNanos (the start of the benchmark).
       * The resolution is unknown. Note that this is only constructed on the
       * master so the initial values before adjustments pertain to the master
       * alone.
       */
      public Timer() {

         // This is just a fake setting of the epochNanos. The call
         // into System.nanoTime() ensures initialization of the nano timer
         // and prevents underflow from calibration.
         epochNanos = System.nanoTime();

         // Set the benchmark epoch 10ms ahead of the actual current time
         // also prevents underflow in case the nano timer is just initialized.
         // The value of the nano timer may be very close to Long.MIN_VALUE.
         epochMillis = System.currentTimeMillis() + 10l;
         epochNanos = calibrateNanos(epochMillis);

         logger.info("Timer: baseTime ms: " + epochMillis +
               ", ns: " + epochNanos);
      }

      /**
       * Calibrates the difference of the nanosec timer from the millisec
       * timer using 100 iterations. This is probably the most accurate
       * way to establish the relationship.
       *
       * @param baseMillis
       *       the base millisec to find the base nanosec for
       * @return The base nanosec corresponding to the base millisec.
       */
      private long calibrateNanos(long baseMillis) {

//            return estimateNanos(baseMillis);

         int iterations = 100;
         int limit = 1000;  // Limit the number of total loops...
         long[] diffms = new long[iterations];
         int[] diffns = new int[iterations];
         int[] clockStep = new int[iterations];
         int msStep = Integer.MAX_VALUE;
         System.gc(); // We don't want gc during calibration. So we try to gc
         // now, on best effort.

         // Run the calibration loop and collect data.
         for (int i = 0, tooLong = 0; i < iterations; ) {
            if (tooLong > limit) // prevent infinite loops.
            {
               throw new RuntimeException("Cannot establish clock offset " +
                     "(ms->ns), retries " + i + "," +
                     tooLong);
            }

            long nanos;
            long millis;
            long millisBefore = System.currentTimeMillis();
            do {
               nanos = System.nanoTime();
               millis = System.currentTimeMillis();
            } while (millis == millisBefore);

            // Now we're on the edge of a new ms value
            // Find the ms clock step for this system
            // by iterating down the min step value.
            clockStep[i] = (int) (millis - millisBefore);
            if (clockStep[i] < msStep) {
               msStep = clockStep[i];
            }

            // If we discover any step bigger than the best recorded step,
            // ignore the iteration.
            if (msStep != Integer.MAX_VALUE && clockStep[i] > msStep) {
               ++tooLong;
               continue;
            }

            diffms[i] = millis - nanos / TO_NANOS;
            diffns[i] = (int) (nanos % TO_NANOS);
            logger.info("iter: " + i + ", millis: " + millis +
                  ", nanos: " + nanos + ", diffms: " + diffms[i] +
                  ", diffns: " + diffns[i] + ", stepms: " + clockStep[i]);
            tooLong = 0; // Reset retry counters on success.
            ++i;
         }
         logger.info("System.currentTimeMillis() granularity is " + msStep +
               "ms");

         // There might still be some records left at the beginning before
         // that have the step > minstep. This happens before the minstep
         // is established. We must not use these records. Count them and
         // report. If more than 25% are bad, don't continue.
         int badRecords = 0;
         for (int i = 0; i < clockStep.length; i++) {
            if (clockStep[i] > msStep) {
               logger.info("Rejected bad record " + i +
                     "Edge mis-detection. Clock step of " +
                     clockStep[i] + "ms higher than granularity.");
               ++badRecords;
            }
         }
         if (badRecords > iterations / 4) {
            throw new RuntimeException("Cannot establish clock offset " +
                  "(ms->ns), edge mis-detections beyond threshold - " +
                  badRecords + " out of " + iterations +
                  ". Perhaps system is too busy.");
         } else {
            logger.info("Rejected " + badRecords + " bad records.");
         }
         int goodRecords = iterations - badRecords;

         // Find the granularity of the nanosec results.
         int granularity = 6;
         for (int i = 0; i < diffns.length; i++) {
            int ns = diffns[i];
            int mod = 10;
            int g = 0;
            for (; g < 6; g++) {
               if (ns % mod != 0) {
                  break;
               }
               mod *= 10;
            }
            if (g < granularity) {
               granularity = g;
            }
            if (granularity == 0) {
               break;
            }
         }
         logger.info("Nanosec timer granularity: 1e" + granularity);

         // Find the max ms difference.
         long maxDiffMs = Long.MIN_VALUE;
         for (int i = 0; i < diffms.length; i++) {
            if (clockStep[i] > msStep) // ignore bad records
            {
               continue;
            }
            if (diffms[i] > maxDiffMs) {
               maxDiffMs = diffms[i];
            }
         }

         // Adjust the ms difference to be the same, the rest goes into ns.
         for (int i = 0; i < diffms.length; i++) {
            if (clockStep[i] > msStep) // again, ignore bad records
            {
               continue;
            }
            if (diffms[i] < maxDiffMs) {
               diffns[i] += (maxDiffMs - diffms[i]) * TO_NANOS;
               diffms[i] = maxDiffMs;
            }
         }

         // Find the avg diffns
         double avgDiffNs = 0d;
         for (int i = 0; i < diffns.length; i++) {
            if (clockStep[i] == msStep) // again, ignore bad records
            {
               avgDiffNs += diffns[i];
            }
         }
         avgDiffNs /= goodRecords;

         // Find the standard deviation
         double sdevDiffNs = 0d;
         for (int i = 0; i < diffns.length; i++) {
            if (clockStep[i] == msStep) { // again, use only good records
               double dev = diffns[i] - avgDiffNs;
               dev *= dev;
               sdevDiffNs += dev;
            }
         }
         sdevDiffNs = Math.sqrt(sdevDiffNs / goodRecords);
         logger.info("Sdev nsec: " + sdevDiffNs);

         // Now, eliminate the outliers beyond 2x sdev.
         // Based on the empirical rule, about 95% of the values are within
         // 2 standard deviations (assuming a normal distribution of
         // timing discrepancy). So what's beyond 2x sdev can really
         // be counted as outliers.
         int count = 0;
         double avgDiffNs2 = 0;
         for (int i = 0; i < diffns.length; i++) {
            if (clockStep[i] > msStep) // again, ignore bad records
            {
               continue;
            }
            double dev = Math.abs(diffns[i] - avgDiffNs);
            if (dev <= sdevDiffNs * 2d) { // Outliers are beyond 2x sdev.
               ++count;
               avgDiffNs2 += diffns[i];
            } else {
               logger.info("Excluded outlier record " + i + ". " +
                     "Nanosec diff beyond 2*sdev or about 95th% " +
                     "according to empirical rule");
            }
         }
         avgDiffNs2 /= count;

         // Warn if we have a lot of outliers, allow 10% on each side.
         if (count < (int) 0.8d * goodRecords) {
            logger.warn("Too many outliers in calibration. " +
                  (goodRecords - count) + " of " + goodRecords);
         }

         // Round the average to the granularity
         int grainSize = 1;
         for (int i = 0; i < granularity; i++) {
            grainSize *= 10;
         }
         int avgDiffNsI = (int) Math.round(avgDiffNs2 / grainSize);
         avgDiffNsI *= grainSize;

         // Re-adjust the ms so the ns does not exceed 1,000,000.
         maxDiffMs -= avgDiffNsI / 1000000;
         avgDiffNsI %= 1000000;

         // Then assign the diffs.
         this.diffms = maxDiffMs;
         this.diffns = avgDiffNsI;

         // Based on our local differences between the nanos and millis clock
         // clock, we calculate the base nanose based on the given base millis.
         return (baseMillis - this.diffms) * TO_NANOS + this.diffns;
      }

      /**
       * Estimates the difference of the nanosec timer from the millisec
       * timer. This method is not as timing sensitive as calibrateNanos
       * and is routed from calibrateNanos in debug mode when the driver
       * is run in an IDE or debugger. It is by far not as accurate as
       * calibrateNanos.
       *
       * @param baseMillis
       *       the base millisec to find the base nanosec for
       * @return The base nanosec corresponding to the base millisec.
       */
      private long estimateNanos(long baseMillis) {
         long ns1 = System.nanoTime();
         long ms = System.currentTimeMillis();
         long ns2 = System.nanoTime();
         long avgNs = (ns2 - ns1) / 2 + ns1;
         this.diffms = ms - avgNs / 1000000;
         this.diffns = (int) (avgNs % 1000000);
         return (baseMillis - this.diffms) * TO_NANOS + this.diffns;
      }

      /**
       * Converts the millisec relative time to absolute nanosecs.
       *
       * @param relTimeMillis
       *       The millisec relative time
       * @return The corresponding nanosec time
       */
      public long toAbsNanos(int relTimeMillis) {
         return (relTimeMillis + epochMillis - diffms) * TO_NANOS + diffns;
      }

      /**
       * Converts the nanosecond time relative to the run's epoch to absolute
       * millisec comparable to System.currentTimeMillis().
       *
       * @param relTimeNanos
       *       The relative time in nanosecs
       * @return The absolute time in millisecs
       */
      public long toAbsMillis(long relTimeNanos) {
         return (relTimeNanos + epochNanos) / TO_NANOS + diffms;
      }

      /**
       * Converts the millisec time relative to the run's epoch to absolute
       * millisec comparable to System.currentTimeMillis().
       *
       * @param relTimeMillis
       *       The relative time in nanosecs
       * @return the absolute time in millisecs
       */
      public long toAbsMillis(int relTimeMillis) {
         return relTimeMillis + epochMillis;
      }

      /**
       * Obtains the current time relative to the base time, in
       * milliseconds. This is mainly used to determine the current state of
       * of the run. The base time is synchronized between the Faban
       * master and all driver agents.
       *
       * @return The nanoseconds from the base time.
       */
      public int getTime() {
         long c = System.currentTimeMillis();
         return (int) (c - epochMillis);
      }

      /**
       * Obtains the time relative to the base time, given a nanoTime
       * with an unknown datum. The base time is synchronized between the Faban
       * master and all driver agents.
       *
       * @param nanoTime
       *       The nanotime obtained from System.nanoTime()
       * @return The nanosecond time relative to our base time.
       */
      public long toRelTime(long nanoTime) {
         return nanoTime - epochNanos;
      }

      /**
       * Obtains the nano time comparable to System.nanoTime() from a given
       * nanotime relative to the base time.
       *
       * @param relNanos
       *       The relative nanosecond time
       * @return The nanoseconds comparable to System.nanoTime.
       */
      public long toAbsTime(long relNanos) {
         return relNanos + epochNanos;
      }

      /**
       * Reads the compensation value.
       *
       * @return The compensation
       */
      public long getCompensation() {
         return compensation;
      }

   }

   @Test(enabled = false)
   public void timerTest() {
      Timer t = new Timer();

      System.out.println(t.toAbsNanos(t.getTime()));
      long last = System.nanoTime(), curr, diff = Long.MAX_VALUE;
      for (int i = 0; i < 10000; i++) {
         curr = System.nanoTime();
         if (curr - last < diff) {
            diff = curr - last;
         }
         last = curr;
      }
      System.out.println(diff);
   }

}
