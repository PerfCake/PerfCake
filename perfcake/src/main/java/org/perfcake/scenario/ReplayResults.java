/*
 * -----------------------------------------------------------------------\
 * SilverWare
 *  
 * Copyright (C) 2014 - 2016 the original author or authors.
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
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.Reporter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ReplayResults implements Closeable {

   private static final Logger log = LogManager.getLogger(ReplayResults.class);

   private FileInputStream inputStream;

   private Map<Reporter, Map<Destination, Long>> reportLastTimes = new HashMap<>();

   private ReportManager reportManager;

   private RunInfo runInfo;

   private long lastIteration = -1;

   public ReplayResults(final Scenario scenario, final String rawRecords) throws IOException {
      inputStream = new FileInputStream(rawRecords);

      reportManager = scenario.getReportManager();
      runInfo = reportManager.getRunInfo();

      // provide our own ReplayRunInfo

      reportManager.getReporters().forEach(reporter -> {
         final Map<Destination, Long> lastTimes = new HashMap<>();
         reportLastTimes.put(reporter, lastTimes);

         reporter.getReportingPeriods().forEach(boundPeriod -> {
            lastTimes.put(boundPeriod.getBinding(), 0L);
         });
         reporter.start();
      });
   }

   public void replay() throws IOException {
      try (
            final ObjectInputStream ois = new ObjectInputStream(inputStream);
      ) {
         final RunInfo originalRunInfo = (RunInfo) ois.readObject();
         final ReplayRunInfo replayRunInfo = new ReplayRunInfo(originalRunInfo);
         reportManager.setRunInfo(replayRunInfo);

         while (inputStream.available() > 0) {
            report(MeasurementUnit.streamIn(ois));
         }
      } catch (ClassNotFoundException cnfe) {
         throw new IOException("Unknown class in the recorded data. Make sure all the plugins are loaded that were used during recording: ", cnfe);
      }
   }

   private void report(final MeasurementUnit mu) {
      if (mu.getIteration() == 0 && lastIteration > 0) { // probably end of warmUp
         resetReporters();
      }

      while (runInfo.getIteration() < mu.getIteration()) {
         runInfo.getNextIteration();
      }

      reportManager.getReporters().forEach(reporter -> {
         try {
            reporter.report(mu);
         } catch (ReportingException e) {
            log.warn("Unable to report result: ", e);
         }
      });

      publishResults(mu.getStopTime() / 1_000_000);

      lastIteration = mu.getIteration();
   }

   private void publishResults(final long currentTime) {
      reportManager.getReporters().forEach(reporter -> {
         reporter.getReportingPeriods().forEach(boundPeriod -> {
            if (boundPeriod.getPeriodType() == PeriodType.TIME) {
               if (reportLastTimes.get(reporter).get(boundPeriod.getBinding()) + boundPeriod.getPeriod() < currentTime) {
                  reportLastTimes.get(reporter).put(boundPeriod.getBinding(), currentTime);
                  try {
                     reporter.publishResult(boundPeriod.getPeriodType(), boundPeriod.getBinding());
                  } catch (ReportingException e) {
                     log.warn("Unable to publish result: ", e);
                  }
               }
            }
         });
      });
   }

   private void resetReporters() {
      reportManager.getReporters().forEach(Reporter::reset);

      reportLastTimes.forEach((reporter, boundPeriod) -> {
         boundPeriod.keySet().forEach(destination -> {
            boundPeriod.put(destination, 0L);
         });
      });
   }

   @Override
   public void close() throws IOException {
      reportManager.getReporters().forEach(Reporter::stop);
      inputStream.close();
   }

   private static class ReplayRunInfo extends RunInfo {

      private final RunInfo originalRunInfo;

      private ReplayRunInfo(final RunInfo originalRunInfo) {
         super(originalRunInfo.getDuration());

         this.originalRunInfo = originalRunInfo;
      }
   }
}
