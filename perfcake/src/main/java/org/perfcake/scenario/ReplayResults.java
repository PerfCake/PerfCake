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
package org.perfcake.scenario;

import org.perfcake.RunInfo;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destination.Destination;
import org.perfcake.reporting.reporter.RawReporter;
import org.perfcake.reporting.reporter.Reporter;
import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Replays the results previously recorded with {@link org.perfcake.reporting.reporter.RawReporter}. The same
 * scenario should be used, and its reporting section will be used to configure reporters and destinations.
 * Then the normal reporting operation is emulated to achieve repeatable results.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ReplayResults implements Closeable {

   /**
    * The logger.
    */
   private static final Logger log = LogManager.getLogger(ReplayResults.class);

   /**
    * Reading the recorded and serialized Measurement Units.
    */
   private FileInputStream inputStream;

   /**
    * Unzipper of the data.
    */
   private GZIPInputStream gzip;

   /**
    * We need to remember when we reported for the last time to emulate time based reporting.
    */
   private Map<Reporter, Map<Destination, Long>> reportLastTimes = new HashMap<>();

   /**
    * Link to {@link ReportManager} used to report results.
    */
   private ReportManager reportManager;

   /**
    * Fake information about the run.
    */
   private ReplayRunInfo runInfo;

   /**
    * Artificial test start time.
    */
   private long firstTime = -1;

   /**
    * Artificial test last reporting time.
    */
   private long lastTime = -1;

   /**
    * Gets a new replay facility for the given scenario and data file reported by {@link RawReporter}.
    *
    * @param scenario
    *       The scenario from which the reporting configuration will be taken.
    * @param rawRecords
    *       The file wit recorded results.
    * @throws IOException
    *       When it was not possible to read the recorded results.
    */
   public ReplayResults(final Scenario scenario, final String rawRecords) throws IOException {
      inputStream = new FileInputStream(rawRecords);
      gzip = new GZIPInputStream(inputStream);

      reportManager = scenario.getReportManager();

      reportManager.getReporters().forEach(reporter -> {
         if (!(reporter instanceof RawReporter)) { // we do not want to cycle or rewrite the results
            final Map<Destination, Long> lastTimes = new HashMap<>();
            reportLastTimes.put(reporter, lastTimes);

            reporter.getReportingPeriods().forEach(boundPeriod -> {
               lastTimes.put(boundPeriod.getBinding(), 0L);
            });
            reporter.start();
         }
      });

      Utils.initTimeStamps();
   }

   /**
    * Replays the recorded data through the scenario's reporters. Any instance of {@link RawReporter} is ignored.
    *
    * @throws IOException
    *       When there was an error reading the replay data.
    */
   public void replay() throws IOException {
      try (
            final ObjectInputStream ois = new ObjectInputStream(gzip);
      ) {
         final RunInfo originalRunInfo = (RunInfo) ois.readObject();
         runInfo = new ReplayRunInfo(originalRunInfo);
         reportManager.setRunInfo(runInfo);

         try {
            while (gzip.available() > 0) {
               report(MeasurementUnit.streamIn(ois));
            }
         } catch (EOFException eof) {
            // nothing wrong, we just stop reporting here, gzip actually keeps some excessive buffer at the end
         }

         resetLastTimes();
         publishResults(lastTime);
      } catch (ClassNotFoundException cnfe) {
         throw new IOException("Unknown class in the recorded data. Make sure all the plugins are loaded that were used during recording: ", cnfe);
      }
   }

   /**
    * Reports the recorded {@link MeasurementUnit} through the scenario's reporters. All instances of {@link RawReporter} are ignored.
    * Needs to emulate time flow because the replay runs faster than the original data recording.
    *
    * @param mu
    *       Thhe {@link MeasurementUnit} to be reported.
    */
   private void report(final MeasurementUnit mu) {
      runInfo.getNextIteration();

      if (firstTime == -1) {
         firstTime = mu.getStartTime();
      }
      runInfo.moveTime(mu);

      reportManager.getReporters().forEach(reporter -> {
         if (!(reporter instanceof RawReporter)) { // we do not want to cycle or rewrite our results
            try {
               reporter.report(mu);
            } catch (ReportingException e) {
               log.warn("Unable to report result: ", e);
            }
         }
      });

      lastTime = (mu.getStopTime() - firstTime) / 1_000_000;
      publishResults(lastTime);
   }

   /**
    * Takes care of publishing via time-bound destinations. This is only triggered by reading the next {@link MeasurementUnit} from
    * the input file. This is a different to real test where we simply report at the given time periods no matter if we have
    * any new recorded data or not. In a faster replay mode, this cannot be emulated.
    *
    * @param currentTime
    *       Artificial time.
    */
   private void publishResults(final long currentTime) {
      reportManager.getReporters().forEach(reporter -> {
         if (!(reporter instanceof RawReporter)) {
            reporter.getReportingPeriods().forEach(boundPeriod -> {
               if (boundPeriod.getPeriodType() == PeriodType.TIME) {
                  if (reportLastTimes.get(reporter).get(boundPeriod.getBinding()) == 0L || reportLastTimes.get(reporter).get(boundPeriod.getBinding()) + boundPeriod.getPeriod() < currentTime) {
                     reportLastTimes.get(reporter).put(boundPeriod.getBinding(), currentTime);
                     try {
                        reporter.publishResult(boundPeriod.getPeriodType(), boundPeriod.getBinding());
                     } catch (ReportingException e) {
                        log.warn("Unable to publish result: ", e);
                     }
                  }
               }
            });
         }
      });
   }

   /**
    * Resets all reporters and remembered reporting periods. This is a callback from the {@link ReplayRunInfo}.
    */
   private void resetReporters() {
      reportManager.getReporters().stream().filter(reporter -> !(reporter instanceof RawReporter)).forEach(Reporter::reset);
      resetLastTimes();
   }

   /**
    * Resets all remembered reporting periods.
    */
   private void resetLastTimes() {
      reportLastTimes.forEach((reporter, boundPeriod) -> {
         boundPeriod.keySet().forEach(destination -> {
            boundPeriod.put(destination, 0L);
         });
      });
   }

   @Override
   public void close() throws IOException {
      reportManager.getReporters().stream().filter(reporter -> !(reporter instanceof RawReporter)).forEach(Reporter::stop);
      inputStream.close();
   }

   /**
    * A replacement of original {@link RunInfo} that allows us to create a notion of
    * artificial time flow and report time bounded results in a faster replay mode.
    */
   private class ReplayRunInfo extends RunInfo {

      /**
       * Current artificial time.
       */
      private long time;

      /**
       * Gets a new replay run information based on the original one stored in the replay data header.
       *
       * @param originalRunInfo
       *       The original {@link RunInfo}.
       */
      private ReplayRunInfo(final RunInfo originalRunInfo) {
         super(originalRunInfo.getDuration());

         try {
            final Field startTimeField = RunInfo.class.getDeclaredField("startTime");
            startTimeField.setAccessible(true);
            startTimeField.set(this, originalRunInfo.getStartTime());

            final Field endTimeField = RunInfo.class.getDeclaredField("endTime");
            endTimeField.setAccessible(true);
            endTimeField.set(this, originalRunInfo.getEndTime());
         } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Unable to properly configure the replay run information: ", e);
         }
      }

      /**
       * Keeps the artificial time clock ticking.
       *
       * @param mu
       *       Another {@link MeasurementUnit} to count/estimate the current artificial time.
       */
      private void moveTime(final MeasurementUnit mu) {
         time = Math.max(time, (mu.getStopTime() - firstTime) / 1_000_000);
      }

      @Override
      public long getRunTime() {
         return time; // returns the artificial clock's time
      }

      @Override
      public void reset() {
         super.reset();
         resetReporters(); // callback to reset remembered reporting time periods
      }
   }
}
