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

import org.perfcake.PerfCakeConst;
import org.perfcake.agent.AgentCommand;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * Reports memory usage information from a remote JVM,
 * where PerfCake agent is deployed. It communicates with the PerfCake agent to
 * get the information via TCP sockets.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class MemoryUsageReporter extends AbstractReporter {

   /**
    * MiB to bytes conversion factor.
    */
   private static final long BYTES_IN_MIB = 1_048_576L;

   /**
    * The reporter's logger.
    */
   private static final Logger log = LogManager.getLogger(MemoryUsageReporter.class);

   /**
    * Hostname where PerfCake agent is listening on.
    */
   private String agentHostname = "localhost";

   /**
    * Port number where PerfCake agent is listening on.
    */
   private String agentPort = "8850";

   /**
    * IP address of the PerfCake agent.
    */
   private InetAddress host;

   /**
    * Socket used to communicate with the PerfCake agent.
    */
   private Socket socket;

   /**
    * Reader to read response from PerfCake agent.
    */
   private BufferedReader responseReader;

   /**
    * Writer to send requests to PerfCake agent.
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
    * Determines the period in which a memory usage is gathered from the PerfCake agent.
    */
   private long memoryLeakDetectionMonitoringPeriod = 500L;

   /**
    * Allows enabling/disabling of garbage collection performed each time the memory usage of the
    * tested system is measured and published.
    * Since the garbage collection is CPU intensive operation be careful to enable it and to how often
    * the memory usage is measured because it will have a significant impact on the measured system and naturally the
    * measured results too. It is disabled (set to <code>false</code>) by default.
    */
   private boolean performGcOnMemoryUsage = false;

   /**
    * A flag that indicates that a possible memory leak has been detected.
    */
   private boolean memoryLeakDetected = false;

   /**
    * A flag that indicates that a heap dump was saved after a possible memory leak has been detected.
    */
   private boolean heapDumpSaved = false;

   /**
    * Tha latest computed used memory trend slope value.
    */
   private float memoryTrendSlope = 0;

   /**
    * The property to make a memory dump, when possible memory leak is detected. The {@link org.perfcake.reporting.reporters.MemoryUsageReporter}
    * will send a command to PerfCake agent that will create a heap dump.
    */
   private boolean memoryDumpOnLeak = false;

   /**
    * The name of the memory dump file created by PerfCake agent.
    */
   private String memoryDumpFile = null;

   /**
    * Gathers memory data for memory leak detection analysis.
    */
   private MemoryDataGatheringTask memoryDataGatheringTask = null;

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(final String key, final Class clazz) {
      return new LastValueAccumulator();
   }

   @Override
   protected void doReset() {
      if (usedMemoryTimeWindow != null) {
         usedMemoryTimeWindow.clear();
      }
      heapDumpSaved = false;
   }

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
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
         socket = new Socket(host, Integer.parseInt(agentPort));
         requestWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Utils.getDefaultEncoding()), true);
         responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), Utils.getDefaultEncoding()));
         usedMemoryTimeWindow = new LinkedBlockingQueue<>(usedMemoryTimeWindowSize);

         if (memoryLeakDetectionEnabled) { // start the thread only if memory leak detection is enabled
            memoryDataGatheringTask = new MemoryDataGatheringTask();
            final Thread memoryDataGatheringThread = new Thread(memoryDataGatheringTask);
            memoryDataGatheringThread.setName("PerfCake-memory-data-gathering-thread");
            memoryDataGatheringThread.setDaemon(true);
            memoryDataGatheringThread.start();
         }
      } catch (final IOException ioe) {
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
      } catch (final IOException ioe) {
         ioe.printStackTrace();
      }
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination destination) throws ReportingException {
      try {
         final Measurement m = newMeasurement();
         if (performGcOnMemoryUsage) {
            sendAgentCommand(AgentCommand.GC.name());
         }
         final long used = sendAgentCommand(AgentCommand.USED.name());
         m.set("Used", (new Quantity<Number>((double) used / BYTES_IN_MIB, "MiB")));
         m.set("Total", (new Quantity<Number>((double) sendAgentCommand(AgentCommand.TOTAL.name()) / BYTES_IN_MIB, "MiB")));
         m.set("Max", (new Quantity<Number>((double) sendAgentCommand(AgentCommand.MAX.name()) / BYTES_IN_MIB, "MiB")));
         if (memoryLeakDetectionEnabled) {
            if (usedMemoryTimeWindow.size() == usedMemoryTimeWindowSize) {
               m.set("UsedTrend", new Quantity<Number>(memoryTrendSlope, "B/s"));
               m.set("MemoryLeak", memoryLeakDetected);
            } else {
               m.set("UsedTrend", null);
               m.set("MemoryLeak", null);
            }
         }
         destination.report(m);
         if (log.isDebugEnabled()) {
            log.debug("Reporting: [" + m.toString() + "]");
         }
      } catch (final IOException ioe) {
         throw new ReportingException("Could not publish result", ioe);
      }
   }

   /**
    * Gathers memory data internally for memory leak detection analysis.
    */
   private class MemoryDataGatheringTask implements Runnable {

      private boolean running;

      @Override
      public void run() {
         this.running = true;
         long used = -1L;
         while (running) {
            try {
               used = sendAgentCommand(AgentCommand.USED.name());
            } catch (final IOException e) {
               e.printStackTrace();
            }

            if (usedMemoryTimeWindow.size() == usedMemoryTimeWindowSize) {
               usedMemoryTimeWindow.remove();
            }
            usedMemoryTimeWindow.offer(new TimestampedRecord<Number>(runInfo.getRunTime(), used));
            memoryTrendSlope = (float) Utils.computeRegressionTrend(usedMemoryTimeWindow);
            if (usedMemoryTimeWindow.size() == usedMemoryTimeWindowSize && memoryTrendSlope > memoryLeakSlopeThreshold) {
               memoryLeakDetected = true;
               if (memoryDumpOnLeak && !heapDumpSaved) {
                  try {
                     final StringBuffer cmd = new StringBuffer();
                     cmd.append(AgentCommand.DUMP.name());
                     cmd.append(":");
                     if (memoryDumpFile != null) {
                        cmd.append(memoryDumpFile);
                     } else {
                        cmd.append("dump-" + System.getProperty(PerfCakeConst.TIMESTAMP_PROPERTY) + ".bin");
                     }
                     if (sendAgentCommand(cmd.toString()) < 0) {
                        throw new RuntimeException("An exception occured at PerfCake agent side.");
                     }
                     heapDumpSaved = true;
                  } catch (final IOException e) {
                     e.printStackTrace();
                  }
               }

            }
            try {
               Thread.sleep(memoryLeakDetectionMonitoringPeriod);
            } catch (final InterruptedException e) {
               e.printStackTrace();
            }
         }

      }

      /**
       * Stops the task from running.
       */
      public void stop() {
         this.running = false;
      }
   }

   /**
    * Sends a command to the PerfCake agent the reporter is connected to.
    *
    * @param command
    *       PerfCake agent command.
    * @return Command response code.
    * @throws IOException
    *       In the case of communication error.
    */
   private long sendAgentCommand(final String command) throws IOException {
      if (log.isTraceEnabled()) {
         log.trace("sending " + command);
      }
      requestWriter.println(command);
      final long retVal = Long.parseLong(responseReader.readLine());
      if (log.isTraceEnabled()) {
         log.trace("received " + retVal);
      }
      return retVal;
   }

   /**
    * Gets the agent hostname.
    *
    * @return The agent hostname.
    */
   public String getAgentHostname() {
      return agentHostname;
   }

   /**
    * Sets the agent hostname.
    *
    * @param agentHostname
    *       The agent hostname.
    * @return Instance of this to support fluent API.
    */
   public MemoryUsageReporter setAgentHostname(final String agentHostname) {
      this.agentHostname = agentHostname;
      return this;
   }

   /**
    * Gets the agent port.
    *
    * @return The agent port.
    */
   public String getAgentPort() {
      return agentPort;
   }

   /**
    * Sets the agent port.
    *
    * @param agentPort
    *       The agent port.
    * @return Instance of this to support fluent API.
    */
   public MemoryUsageReporter setAgentPort(final String agentPort) {
      this.agentPort = agentPort;
      return this;
   }

   /**
    * Gets the size of the memory time window (number of latest records) for possible memory leak detection.
    *
    * @return The used memory time window size.
    */
   public int getUsedMemoryTimeWindowSize() {
      return usedMemoryTimeWindowSize;
   }

   /**
    * Sets the size of the memory time window (number of latest records) for possible memory leak detection.
    *
    * @param timeWindowSize
    *       The used memory time window size.
    * @return Instance of this to support fluent API.
    */
   public MemoryUsageReporter setUsedMemoryTimeWindowSize(final int timeWindowSize) {
      this.usedMemoryTimeWindowSize = timeWindowSize;
      return this;
   }

   /**
    * Gets the possible memory leak detection threshold. Possible memory leak is found,
    * when the actual slope of the linear regression line computed from the time window data set is greater than the threshold.
    *
    * @return The memory leak slope threshold.
    */
   public double getMemoryLeakSlopeThreshold() {
      return memoryLeakSlopeThreshold;
   }

   /**
    * Sets the possible memory leak detection threshold. Possible memory leak is found,
    * when the actual slope of the linear regression line computed from the time window data set is greater than the threshold.
    *
    * @param memoryLeakSlopeThreshold
    *       The memory leak slope threshold.
    * @return Instance of this to support fluent API.
    */
   public MemoryUsageReporter setMemoryLeakSlopeThreshold(final double memoryLeakSlopeThreshold) {
      this.memoryLeakSlopeThreshold = memoryLeakSlopeThreshold;
      return this;
   }

   /**
    * Is the possible memory leak detection enabled?
    *
    * @return The memory leak detection status.
    */
   public boolean isMemoryLeakDetectionEnabled() {
      return memoryLeakDetectionEnabled;
   }

   /**
    * Enables/disables the possible memory leak detection mechanism.
    *
    * @param memoryLeakDetectionEnabled
    *       <code>true</code> to enable memory leak detection mechanism.
    * @return Instance of this to support fluent API.
    */
   public MemoryUsageReporter setMemoryLeakDetectionEnabled(final boolean memoryLeakDetectionEnabled) {
      this.memoryLeakDetectionEnabled = memoryLeakDetectionEnabled;
      return this;
   }

   /**
    * Gets the period in which a memory usage is gathered from the PerfCake agent.
    *
    * @return The memory leak detection monitoring period.
    */
   public long getMemoryLeakDetectionMonitoringPeriod() {
      return memoryLeakDetectionMonitoringPeriod;
   }

   /**
    * Sets  the period in which a memory usage is gathered from the PerfCake agent.
    *
    * @param memoryLeakDetectionMonitoringPeriod
    *       The memory leak detection monitoring period.
    * @return Instance of this to support fluent API.
    */
   public MemoryUsageReporter setMemoryLeakDetectionMonitoringPeriod(final long memoryLeakDetectionMonitoringPeriod) {
      this.memoryLeakDetectionMonitoringPeriod = memoryLeakDetectionMonitoringPeriod;
      return this;
   }

   /**
    * Gets the name of the memory dump file created by PerfCake agent.
    *
    * @return Name of the memory dump file.
    */
   public String getMemoryDumpFile() {
      return memoryDumpFile;
   }

   /**
    * Sets a the name of the memory dump file created by PerfCake agent.
    *
    * @param memoryDumpFile
    *       Memory dump file name.
    * @return Instance of this to support fluent API.
    */
   public MemoryUsageReporter setMemoryDumpFile(final String memoryDumpFile) {
      this.memoryDumpFile = memoryDumpFile;
      return this;
   }

   /**
    * Returns a value of the property to make a memory dump, when possible memory leak is detected. The {@link org.perfcake.reporting.reporters.MemoryUsageReporter}
    * will send a command to PerfCake agent that will create a heap dump.
    *
    * @return <code>true</code> if the memory dump on leak is enabled. <code>false</code> otherwise.
    */
   public boolean isMemoryDumpOnLeak() {
      return memoryDumpOnLeak;
   }

   /**
    * Sets the value of the property to make a memory dump, when possible memory leak is detected. The {@link org.perfcake.reporting.reporters.MemoryUsageReporter}
    * will send a command to PerfCake agent that will create a heap dump.
    *
    * @param memoryDumpOnLeak
    *       Enables or disables the memory dump on leak.
    * @return Instance of this to support fluent API.
    */
   public MemoryUsageReporter setMemoryDumpOnLeak(final boolean memoryDumpOnLeak) {
      this.memoryDumpOnLeak = memoryDumpOnLeak;
      return this;
   }

   /**
    * Returns the value of the property that indicate if performing garbage collection (each time the memory usage of the
    * tested system is measured and published) is enabled or disabled.
    *
    * @return <code>true</code> if the garbage collection feature is enabled, <code>false</code> otherwise.
    */
   public boolean isPerformGcOnMemoryUsage() {
      return performGcOnMemoryUsage;
   }

   /**
    * Enables/disables garbage collection to be performed each time the memory usage of the
    * tested system is measured and published.
    * Since the garbage collection is CPU intensive operation be careful to enable it and to how often
    * the memory usage is measured because it will have a significant impact on the measured system and naturally the
    * measured results too.
    *
    * It is disabled by default.
    *
    * @param performGcOnMemoryUsage
    *       <code>true</code> to enable the feature. The <code>false</code> otherwise.
    * @return Instance of this to support fluent API.
    */
   public MemoryUsageReporter setPerformGcOnMemoryUsage(final boolean performGcOnMemoryUsage) {
      this.performGcOnMemoryUsage = performGcOnMemoryUsage;
      return this;
   }
}
