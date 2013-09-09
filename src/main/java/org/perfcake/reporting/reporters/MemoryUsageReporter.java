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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;
import org.perfcake.util.agent.PerfCakeAgent;
import org.perfcake.util.agent.PerfCakeAgent.Memory;

/**
 * Reporter that is able to get the memory usage information from a remote JVM,
 * where {@link PerfCakeAgent} is deployed. It communicates with the {@link PerfCakeAgent} to
 * get the information.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
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
   private String agentPort = "8849";

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
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      try {
         Measurement m = newMeasurement();
         m.set("Used", (new Quantity<Number>((double) getMemoryUsage(Memory.USED) / BYTES_IN_MIB, "MiB")));
         m.set("Total", (new Quantity<Number>((double) getMemoryUsage(Memory.TOTAL) / BYTES_IN_MIB, "MiB")));
         m.set("Max", (new Quantity<Number>((double) getMemoryUsage(Memory.MAX) / BYTES_IN_MIB, "MiB")));
         d.report(m);
         if (log.isDebugEnabled()) {
            log.debug("Reporting: [" + m.toString() + "]");
         }
      } catch (IOException ioe) {
         throw new ReportingException("Could not publish result", ioe);
      }
   }

   /**
    * Gets the memory usage information from the {@link PerfCakeAgent} the reporter is connected to.
    * 
    * @param type
    *           {@link Memory} type.
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
    *           The agent hostname value to set.
    */
   public void setAgentHostname(final String agentHostname) {
      this.agentHostname = agentHostname;
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
    *           The agent port value to set.
    */
   public void setAgentPort(final String agentPort) {
      this.agentPort = agentPort;
   }

}
