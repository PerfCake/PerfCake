package org.perfcake.reporting.reporters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.log4j.Logger;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;
import org.perfcake.util.PerfCakeAgent;
import org.perfcake.util.PerfCakeAgent.Memory;

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
   private static final NumberFormat numberFormat = new DecimalFormat("0.00");

   /**
    * The reporter's logger.
    */
   private static final Logger log = Logger.getLogger(MemoryUsageReporter.class);

   /**
    * Hostname where {@link PerfCakeAgent} is listening on.
    */
   private String hostname = "localhost";

   /**
    * Port number where {@link PerfCakeAgent} is listening on.
    */
   private String port = "8849";

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

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.reporters.AbstractReporter#getAccumulator(java.lang.String, java.lang.Class)
    */
   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(String key, Class clazz) {
      return new LastValueAccumulator();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.reporters.AbstractReporter#doReset()
    */
   @Override
   protected void doReset() {
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.reporters.AbstractReporter#doReport(org.perfcake.nreporting.MeasurementUnit)
    */
   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.reporters.AbstractReporter#start()
    */
   @Override
   public void start() {
      super.start();
      try {
         host = InetAddress.getByName(hostname);
         if (log.isDebugEnabled()) {
            log.debug("Creating socket " + host + ":" + port + "...");
         }
         socket = new Socket(host, Integer.valueOf(port));
         requestWriter = new PrintWriter(socket.getOutputStream(), true);
         responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.reporters.AbstractReporter#stop()
    */
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

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.reporters.AbstractReporter#doPublishResult(org.perfcake.common.PeriodType, org.perfcake.nreporting.destinations.Destination)
    */
   @Override
   protected void doPublishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      try {
         Measurement m = new Measurement((long) runInfo.getPercentage(), runInfo.getRunTime(), runInfo.getIteration());
         long used = getMemoryUsage(Memory.USED);
         // m.set(new Quantity<Number>((double) used / BYTES_IN_MIB, "MiB"));
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
    * Used to read the value of hostname.
    * 
    * @return The hostname value.
    */
   public String getHostname() {
      return hostname;
   }

   /**
    * Used to set the value of hostname.
    * 
    * @param hostname
    *           The hostname value to set.
    */
   public void setHostname(final String hostname) {
      this.hostname = hostname;
   }

   /**
    * Used to read the value of port.
    * 
    * @return The port value.
    */
   public String getPort() {
      return port;
   }

   /**
    * Used to set the value of port.
    * 
    * @param port
    *           The port value to set.
    */
   public void setPort(final String port) {
      this.port = port;
   }

}
