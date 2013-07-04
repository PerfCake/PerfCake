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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.perfcake.message.generator.types.MemoryRecord;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementTypes;
import org.perfcake.reporting.ReportsException;
import org.perfcake.reporting.destinations.ConsoleDestination;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.util.LinearRegression;

/**
 * <p>
 * This reporter reports KPI of ESB server. Now it reports just memory :). It also computes computes regression of memoryWindow.
 * </p>
 * <p/>
 * <p>
 * This reporter allows following properties
 * <table border="1">
 * <tr>
 * <td>Property name</td>
 * <td>Description</td>
 * <td>Required</td>
 * <td>Sample value</td>
 * <td>Default</td>
 * </tr>
 * <tr>
 * <td>m_window_size</td>
 * <td>size of window for memroy regression</td>
 * <td>NO</td>
 * <td>100</td>
 * <td>1024</td>
 * </tr>
 * </table>
 * <p/>
 * </p>
 * This reporter reports into 3 csv files, one for Used memory and second for
 * Total memory and third for regressions
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 */
public class KPIReporter extends Reporter {
   private static final Logger log = Logger.getLogger(KPIReporter.class);

   private static final String PROP_MEMORY_WINDOW_SIZE = "m_window_size";

   private static final int PROGRESS_LENGTH = 40;

   private int memoryWindowSize = 1024;

   private Queue<MemoryRecord> memoryWindow = new LinkedBlockingQueue<MemoryRecord>(memoryWindowSize);

   private Object computeLock = new Object();

   private NumberFormat percentForm = new DecimalFormat("0.00%");

   /** PerfCakeAgent */
   private InetAddress host;

   private int port;

   private Socket socket;

   private BufferedReader responseReader;

   private PrintWriter requestWriter;

   private enum Information {
      FREE_MEMORY, MAX_MEMORY, TOTAL_MEMORY, USED_MEMORY
   }

   /**
    * Time at which the current memory record was recorded
    */
   private long timeMemoryMeasurement = 0;

   // private SecurityManager sm;
   private Measurement totalMemory = null;

   private Measurement usedMemory = null;

   private Measurement regression = null;

   private MemoryRecord currentMemoryRecord = null;

   @Override
   public void loadConfigVals() throws ReportsException {
      try {
         host = InetAddress.getByName(getStringProperty("agent-host", System.getProperty("jbossas.server.host")));
         port = getIntProperty("agent-port", 8849);
         if (log.isInfoEnabled()) {
            log.info("Creating socket " + host + ":" + port + "...");
         }
         socket = new Socket(host, port);
         responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         requestWriter = new PrintWriter(socket.getOutputStream(), true);
      } catch (IOException e) {
         e.printStackTrace();
      }

      memoryWindowSize = getIntProperty(PROP_MEMORY_WINDOW_SIZE, 1024);
      memoryWindow = new LinkedBlockingQueue<MemoryRecord>(memoryWindowSize);

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
      List<Measurement> measurements = new ArrayList<Measurement>();
      synchronized (computeLock) {
         compute();
         measurements.add(totalMemory);
         measurements.add(usedMemory);
         measurements.add(regression);
      }

      for (Destination dest : destinations) {
         if (dest instanceof ConsoleDestination) {
            sendToConsole((ConsoleDestination) dest);
         } else {
            dest.addMessagesToSendQueue(measurements);
            dest.send();
         }
      }
   }

   @Override
   public void periodicalTick(Destination dest) throws ReportsException {
      compute();
      List<Measurement> measurements = new ArrayList<Measurement>();
      measurements.add(totalMemory);
      measurements.add(usedMemory);
      measurements.add(regression);

      if (dest instanceof ConsoleDestination) {
         sendToConsole((ConsoleDestination) dest);
      } else {
         dest.addMessagesToSendQueue(measurements);
         dest.send();
      }
   }

   private void compute() throws ReportsException {
      /** Don't measure too often! **/
      synchronized (computeLock) {
         if (System.currentTimeMillis() - timeMemoryMeasurement < 100) {
            return;
         }
         timeMemoryMeasurement = System.currentTimeMillis();

         currentMemoryRecord = getServerMemory();
         String timeOfMemoryMeasurement = testRunInfo.getTestRunTimeString();
         if (memoryWindow.size() == memoryWindowSize) {
            memoryWindow.remove();
         }
         memoryWindow.add(currentMemoryRecord);

         totalMemory = new Measurement(MeasurementTypes.M_TOTAL, getLabelType(), getLabel(), String.valueOf(currentMemoryRecord.getTotal()));
         usedMemory = new Measurement(MeasurementTypes.M_USED, getLabelType(), getLabel(), String.valueOf(currentMemoryRecord.getTotal() - currentMemoryRecord.getFree()));
         regression = new Measurement(MeasurementTypes.M_REGRESSION, getLabelType(), getLabel(), String.valueOf(LinearRegression.computeMemoryUsedRegressionTrend(memoryWindow)));
      }
   }

   private static String progressBar(MemoryRecord memRec, int length) {
      return progressBar(memRec.getTotal(), memRec.getFree(), memRec.getMax(), length);
   }

   private static String progressBar(long total, long free, long max, int length) {
      StringBuffer sb = new StringBuffer();
      int t = (int) (((double) total / max) * length);
      int f = (int) (((double) free / max) * length);
      int i = 0;
      sb.append("[");
      for (; i < t - f; i++) {
         sb.append(")");
      }
      for (; i < t - 1; i++) {
         sb.append(" ");
      }
      sb.append("|");
      i++;
      for (; i < length; i++) {
         sb.append(" ");
      }
      sb.append("]");
      return sb.toString();
   }

   private void sendToConsole(ConsoleDestination consoleDestination) {

      if (currentMemoryRecord != null) {
         StringBuilder sb = new StringBuilder();
         sb.append(progressBar(currentMemoryRecord, PROGRESS_LENGTH));
         sb.append("[");
         sb.append(currentMemoryRecord.getFree());
         sb.append("|");
         sb.append(currentMemoryRecord.getTotal());
         sb.append("|");
         sb.append(currentMemoryRecord.getMax());
         sb.append("|");
         sb.append(LinearRegression.computeMemoryUsedRegressionTrend(memoryWindow));
         sb.append("]");
         sb.append("[" + percentForm.format(testRunInfo.getPercentage()) + "]");

         consoleDestination.outputCustom(sb.toString());
      } else {
         log.warn("No memory record present!");
      }

   }

   /**
    * Gets current memory status
    * 
    * @throws Exception
    */
   private MemoryRecord getServerMemory() {
      try {
         long total = Long.valueOf(getInfo(Information.TOTAL_MEMORY));
         long free = Long.valueOf(getInfo(Information.FREE_MEMORY));
         long max = Long.valueOf(getInfo(Information.MAX_MEMORY));
         return new MemoryRecord(free, total, max);
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   private String getInfo(Information info) throws IOException {
      String request = null;
      switch (info) {
         case FREE_MEMORY:
            request = "getFreeMemory";
            break;
         case TOTAL_MEMORY:
            request = "getTotalMemory";
            break;
         case MAX_MEMORY:
            request = "getMaxMemory";
            break;
         case USED_MEMORY:
            request = "getUsedMemory";
            break;
         default:
            return null;
      }
      requestWriter.println(request);
      return responseReader.readLine();
   }
}
