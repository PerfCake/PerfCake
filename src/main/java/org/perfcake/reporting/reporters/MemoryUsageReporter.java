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
package org.perfcake.reporting.reporters;

import org.perfcake.common.PeriodType;
import org.perfcake.common.TimestampedRecord;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;
import org.perfcake.util.Utils;
import org.perfcake.util.agent.PerfCakeAgent;
import org.perfcake.util.agent.PerfCakeAgent.Memory;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Reporter that is able to get the memory usage information from a remote JVM,
 * where {@link PerfCakeAgent} is deployed. It communicates with the {@link PerfCakeAgent} to
 * get the information.
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class MemoryUsageReporter extends AbstractReporter {

   private static final long BYTES_IN_MIB = 1_048_576L;

   /**
    * The reporter's logger.
    */
   private static final Logger log = Logger.getLogger(MemoryUsageReporter.class);

   /**
    * Hostname where {@link PerfCakeAgent} is listening on.
    */
   private String agentHostname = "localhost";

   /**
    * Port number where {@link PerfCakeAgent} is listening on.
    */
   private String agentPort = "8850";

   /**
    * IP address of the {@link PerfCakeAgent}.
    */
   private InetAddress host;

   /**
    * Socket used to communicate with the {@link PerfCakeAgent}.
    */
   private Socket socket;

   /**
    * Reader to read response from {@link PerfCakeAgent}.
    */
   private BufferedReader responseReader;

   /**
    * Writer to send requests to {@link PerfCakeAgent}.
    */
   private PrintWriter requestWriter;

   /**
    * Used memory time window (number of latest records) for possible memory leak detection.
    */
   private Queue<TimestampedRecord<Number>> usedMemoryTimeWindow;

   /**
    * Size of the memory time window (number of latest records) for possible memory leak detection.
    */
   private int usedMemoryTimeWindowSize = 100;

   /**
    * Possible memory leak detection threshold. Possible memory leak is found, when the actual slope of the linear regression
    * line computed from the time window data set is greater than the threshold.
    */
   private double memoryLeakSlopeThreshold = 1024; // default 1 KiB/s

   /**
    * Switch to enabling (disabling) the possible memory leak detection.
    */
   private boolean memoryLeakDetectionEnabled = false;

   /**
    * Determines the period in which a memory usage is gathered from the {@link PerfCakeAgent}.
    */
   private long memoryLeakDetectionMonitoringPeriod = 500L;

   /**
    * A flag that indicates that a possible memory leak has been detected.
    */
   private boolean memoryLeakDetected = false;

   /**
    * Tha latest computed used memory trend slope value.
    */
   private float memoryTrendSlope = 0;

   private MemoryDataGatheringTask memoryDataGatheringTask = null;

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(final String key, final Class clazz) {
      return new LastValueAccumulator();
   }

   @Override
   protected void doReset() {
      // nop
   }

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      // nop
   }

   @Override
   public void start() {
      super.start();
      try {
         host = InetAddress.getByName(agentHostname);
         if (log.isDebugEnabled()) {
            log.debug("Creating socket " + host + ":" + agentPort + "...");
         }
         socket = new Socket(host, Integer.valueOf(agentPort));
         requestWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), PerfCakeAgent.DEFAULT_ENCODING), true);
         responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), PerfCakeAgent.DEFAULT_ENCODING));
         usedMemoryTimeWindow = new LinkedBlockingQueue<>(usedMemoryTimeWindowSize);

         if (memoryLeakDetectionEnabled) { // start the thread only if memory leak detection is enabled
            memoryDataGatheringTask = new MemoryDataGatheringTask();
            Thread memoryDataGatheringThread = new Thread(memoryDataGatheringTask);
            memoryDataGatheringThread.setName("PerfCake-memory-data-gathering-thread");
            memoryDataGatheringThread.setDaemon(true);
            memoryDataGatheringThread.start();
         }
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
   }

   @Override
   public void stop() {
      super.stop();
      try {
         socket.close();
         requestWriter.close();
         responseReader.close();
         if (memoryLeakDetectionEnabled) {
            memoryDataGatheringTask.stop();
         }
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      try {
         final Measurement m = newMeasurement();
         final long used = getMemoryUsage(Memory.USED);
         m.set("Used", (new Quantity<Number>((double) used / BYTES_IN_MIB, "MiB")));
         m.set("Total", (new Quantity<Number>((double) getMemoryUsage(Memory.TOTAL) / BYTES_IN_MIB, "MiB")));
         m.set("Max", (new Quantity<Number>((double) getMemoryUsage(Memory.MAX) / BYTES_IN_MIB, "MiB")));
         if (memoryLeakDetectionEnabled) {
            if (usedMemoryTimeWindow.size() == usedMemoryTimeWindowSize) {
               m.set("UsedTrend", new Quantity<Number>(memoryTrendSlope, "B/s"));
               m.set("MemoryLeak", memoryLeakDetected);
            } else {
               m.set("UsedTrend", null);
               m.set("MemoryLeak", null);
            }
         }
         d.report(m);
         if (log.isDebugEnabled()) {
            log.debug("Reporting: [" + m.toString() + "]");
         }
      } catch (IOException ioe) {
         throw new ReportingException("Could not publish result", ioe);
      }
   }

   /**
    * An inner class of the task that gathers memory data for memory leak detection analysis.
    */
   private class MemoryDataGatheringTask implements Runnable {

      private boolean running;

      @Override
      public void run() {
         this.running = true;
         long used = -1L;
         while (running) {
            try {
               used = getMemoryUsage(Memory.USED);
            } catch (IOException e) {
               e.printStackTrace();
            }

            if (usedMemoryTimeWindow.size() == usedMemoryTimeWindowSize) {
               usedMemoryTimeWindow.remove();
            }
            usedMemoryTimeWindow.offer(new TimestampedRecord<Number>(runInfo.getRunTime(), used));
            memoryTrendSlope = (float) Utils.computeRegressionTrend(usedMemoryTimeWindow);
            if (usedMemoryTimeWindow.size() == usedMemoryTimeWindowSize && memoryTrendSlope > memoryLeakSlopeThreshold) {
               memoryLeakDetected = true;
            }
            try {
               Thread.sleep(memoryLeakDetectionMonitoringPeriod);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }

      }

      public void stop() {
         this.running = false;
      }
   }

   /**
    * Gets the memory usage information from the {@link PerfCakeAgent} the reporter is connected to.
    *
    * @param type
    *       {@link Memory} type.
    * @return Amount of memory type in bytes.
    * @throws IOException
    */
   private long getMemoryUsage(final Memory type) throws IOException {
      requestWriter.println(type.toString());
      return Long.valueOf(responseReader.readLine());
   }

   /**
    * Used to read the value of agentHostname.
    *
    * @return The agent hostname value.
    */
   public String getAgentHostname() {
      return agentHostname;
   }

   /**
    * Used to set the value of agentHostname.
    *
    * @param agentHostname
    *       The agent hostname value to set.
    */
   public MemoryUsageReporter setAgentHostname(final String agentHostname) {
      this.agentHostname = agentHostname;
      return this;
   }

   /**
    * Used to read the value of agentPort.
    *
    * @return The agent port value.
    */
   public String getAgentPort() {
      return agentPort;
   }

   /**
    * Used to set the value of agentPort.
    *
    * @param agentPort
    *       The agent port value to set.
    */
   public MemoryUsageReporter setAgentPort(final String agentPort) {
      this.agentPort = agentPort;
      return this;
   }

   /**
    * Used to read the value of usedMemoryTimeWindowSize.
    *
    * @return The usedMemoryTimeWindowSize value.
    */
   public int getUsedMemoryTimeWindowSize() {
      return usedMemoryTimeWindowSize;
   }

   /**
    * Used to set the value of timeWindowSize.
    *
    * @param usedMemoryTimeWindowSize
    *       The usedMemoryTimeWindowSize value to set.
    */
   public MemoryUsageReporter setUsedMemoryTimeWindowSize(int timeWindowSize) {
      this.usedMemoryTimeWindowSize = timeWindowSize;
      return this;
   }

   /**
    * Used to read the value of memoryLeakSlopeThreshold.
    *
    * @return The memoryLeakSlopeThreshold value.
    */
   public double getMemoryLeakSlopeThreshold() {
      return memoryLeakSlopeThreshold;
   }

   /**
    * Used to set the value of memoryLeakSlopeThreshold.
    *
    * @param memoryLeakSlopeThreshold
    *       The memoryLeakSlopeThreshold value to set.
    */
   public MemoryUsageReporter setMemoryLeakSlopeThreshold(double memoryLeakSlopeThreshold) {
      this.memoryLeakSlopeThreshold = memoryLeakSlopeThreshold;
      return this;
   }

   /**
    * Used to read the value of memoryLeakDetectionEnabled.
    *
    * @return The memoryLeakDetectionEnabled value.
    */
   public boolean isMemoryLeakDetectionEnabled() {
      return memoryLeakDetectionEnabled;
   }

   /**
    * Used to set the value of memoryLeakDetectionEnabled.
    *
    * @param memoryLeakDetectionEnabled
    *       The memoryLeakDetectionEnabled value to set.
    */
   public MemoryUsageReporter setMemoryLeakDetectionEnabled(boolean memoryLeakDetectionEnabled) {
      this.memoryLeakDetectionEnabled = memoryLeakDetectionEnabled;
      return this;
   }

   /**
    * Used to read the value of memoryLeakDetectionMonitoringPeriod.
    *
    * @return The memoryLeakDetectionMonitoringPeriod value.
    */
   public long getMemoryLeakDetectionMonitoringPeriod() {
      return memoryLeakDetectionMonitoringPeriod;
   }

   /**
    * Used to set the value of memoryLeakDetectionMonitoringPeriod.
    *
    * @param memoryLeakDetectionMonitoringPeriod
    *       The memoryLeakDetectionMonitoringPeriod value to set.
    */
   public MemoryUsageReporter setMemoryLeakDetectionMonitoringPeriod(long memoryLeakDetectionMonitoringPeriod) {
      this.memoryLeakDetectionMonitoringPeriod = memoryLeakDetectionMonitoringPeriod;
      return this;
   }
}
