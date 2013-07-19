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

package org.perfcake.reporting.reporters;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementTypes;
import org.perfcake.reporting.ReportsException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.util.SpeedRecord;

/**
 * AT stands for Average Throughoutput reporter
 * <p>
 * This reporter measures how many iterations/s is the speed of tested component. This reporter simulates old style reporting that used Iteration | Average speed | Current speed. This reporter splits those information into 2 csv files.
 * </p>
 * Sample CSVs:
 * <p>
 * <code>
 * Iteration;Average iteration speed
 * 1632;1439.1534
 * 3397;1591.1007
 * 
 * Iteration;Current iteration speed
 * 1632;1439.1534 
 * 3397;1763.2368
 *  </code>
 * </p>
 * Sample Console output:
 * <p>
 * * Iterations: 5076 (average: 985.8225 iterations/s)
 * </p>
 * 
 * <p>
 * This reporter allows following properties <code>
 * <table border="1">
 *     <tr><td>Property name</td><td>Description</td><td>Required</td><td>Sample value</td><td>Default</td></tr>
 *     <tr><td>time_window_size</td><td>Size of time window for reporting current average throughoutput  </td><td>NO</td><td>4</td><td>16</td></tr>
 * </table>
 * </p>
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class ATReporter extends Reporter {
   private static final String PROP_TIME_WINDOW_SIZE = "time_window_size";

   /**
    * This reporter can periocially output it's results. When it does that it
    * can report current speed which is average speed certain amount of
    * iterations back. This is exactly timeWindowSize. So for example if
    * following vector would be measured performance data: <code>Average speed
    *       2
    *       2
    *       4
    *       4
    *      </code> Then average speed in TimeWindow=1 is 4. In time window 2 it
    * is still 4 but in time window 4 it is (4+4+2+2)/4 = 3.
    */
   private int timeWindowSize = 16;

   /**
    * How many Iterations went through so far
    */
   private static final Logger log = Logger.getLogger(ATReporter.class);

   /**
    * Optimization, do not repeat computations of averages if they are in 100ms
    * window. This is reasonable with this reporter due to it's intent.
    */
   private Queue<SpeedRecord> timeWindow = new LinkedBlockingQueue<SpeedRecord>();

   /**
    * Lock for computing. This monitor should synchronize any modifications of
    * data that are used for computing averages in compute() method.
    */
   private Object computeLock = new Object();

   private Measurement total;

   private Measurement current;

   /**
    * When the last computation of results took place (don't repeat it too
    * often)
    */
   private long lastComputeTime = -1;

   @Override
   public void loadConfigVals() throws ReportsException {
      timeWindowSize = getIntProperty(PROP_TIME_WINDOW_SIZE, 16);
      if (labelingDefault) {
         labelingDefault = false;
         labelingIteration = true;
      }
   }

   @Override
   public void testStarted() {
   }

   @Override
   public void testEnded() throws ReportsException {
      synchronized (computeLock) {
         compute();
         for (Destination dest : destinations) {
            dest.addMessageToSendQueue(total);
            dest.addMessageToSendQueue(current);
            dest.send();
         }
      }

   }

   @Override
   public void periodicalTick(Destination dest) throws ReportsException {
      synchronized (computeLock) {
         compute();
         dest.addMessageToSendQueue(total);
         dest.addMessageToSendQueue(current);
      }
      dest.send();
   }

   /**
    * Computes everything that is necessary before saving anything into any
    * destination :)
    * 
    * This method is here to separate logic of computation from presentation
    * (sending).
    * 
    * @throws ReportsException
    */
   private synchronized void compute() throws ReportsException {
      long currentTime = System.currentTimeMillis();

      if (testRunInfo.testFinished()) {
         currentTime = testRunInfo.getTestEndTime();
      }

      String oAverageThroughoutput = getAverageThroughOutput(currentTime);
      String oTimeWindowsThrougoutput = getCurrentTimeWindowThroughoutput(currentTime);
      total = new Measurement(MeasurementTypes.AI_TOTAL, getLabelType(), getLabel(), oAverageThroughoutput);
      current = new Measurement(MeasurementTypes.AI_CURRENT, getLabelType(), getLabel(), oTimeWindowsThrougoutput);
   }

   /**
    * Helper method for compute()
    * 
    * @param processedCount
    * @param totalTimeMiliseconds
    */
   private String getAverageThroughOutput(long processedCount, long totalTimeMiliseconds) {
      if (totalTimeMiliseconds < 100) {
         return decimalFormat.format(0);
      }
      return decimalFormat.format(1000f * processedCount / totalTimeMiliseconds);
   }

   /**
    * Helper method for compute()
    * 
    * String value of current average.
    * 
    */
   private String getAverageThroughOutput(long currentTime) {
      return getAverageThroughOutput(testRunInfo.getProcessedIterations(), currentTime - testRunInfo.getTestStartTime());
   }

   /**
    * Helper method for compute()
    * 
    * This reporter has set the time window in which he should measure
    * "current speed" to determine whether speed is increasing or descreasing.
    * 
    */
   private String getCurrentTimeWindowThroughoutput(long currentTime) {
      SpeedRecord sr = null;

      if (timeWindow.size() >= timeWindowSize) {
         sr = timeWindow.poll();
      } else {
         sr = new SpeedRecord(testRunInfo.getTestStartTime(), 0);
      }

      timeWindow.add(new SpeedRecord(currentTime, testRunInfo.getProcessedIterations()));
      // log.debug("Getting current timewindow throughoutput. Processed ["+testRunInfo.getProcessedIterations()+"] Runtime ["+testRunInfo.getTestRunTime()+"] Timewindow count ["+sr.getCount()+"] timewindow time ["+(currentTime
      // - sr.getTime())+"]");
      return getAverageThroughOutput(testRunInfo.getProcessedIterations() - sr.getCount(), currentTime - sr.getTime());
   }
}
