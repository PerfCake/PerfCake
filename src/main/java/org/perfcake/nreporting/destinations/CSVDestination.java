package org.perfcake.nreporting.destinations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.perfcake.nreporting.Measurement;
import org.perfcake.nreporting.Quantity;
import org.perfcake.nreporting.ReportingException;
import org.perfcake.nreporting.util.HMSNumberFormat;

/**
 * The destination that appends the {@link Measurement} into a CSV file.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class CSVDestination implements Destination {

   /**
    * Output CSV file path.
    */
   private String path = "";

   /**
    * Output CSV file.
    */
   private File csvFile = null;

   /**
    * CSV delimiter.
    */
   private String delimiter = ";";

   /**
    * Time format in a form of "H:M:S".
    */
   private final HMSNumberFormat timeFormat = new HMSNumberFormat();

   /**
    * The destination's logger.
    */
   private static final Logger log = Logger.getLogger(CSVDestination.class);

   /**
    * The list containing names of results from measurement.
    */
   private List<String> resultNames = new LinkedList<>();

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#open()
    */
   @Override
   public void open() {
      // TODO Auto-generated method stub

   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#close()
    */
   @Override
   public void close() {
      // TODO Auto-generated method stub

   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#report(org.perfcake.nreporting.Measurement)
    */
   @Override
   public void report(final Measurement m) throws ReportingException {
      StringBuffer sb = new StringBuffer();
      if (csvFile == null) {
         csvFile = new File(path);
         if (log.isDebugEnabled()) {
            log.debug("Output path not specified. Using the default one: " + csvFile.getPath());
         }
      }

      final Map<String, Object> results = m.getAll();

      // make sure the order of columns is consisten
      if (!csvFile.exists()) {
         sb.append("Time");
         sb.append(delimiter);
         sb.append("Iterations");
         sb.append(delimiter);
         sb.append(Measurement.DEFAULT_RESULT);
         for (String key : results.keySet()) {
            if (!key.equals(Measurement.DEFAULT_RESULT)) {
               resultNames.add(key);
               sb.append(delimiter);
               sb.append(key);
            }
         }
         sb.append("\n");
      }

      sb.append(timeFormat.format(m.getTime()));
      sb.append(delimiter);
      sb.append(m.getIteration());
      sb.append(delimiter);
      Object defaultResult = m.get();
      if (defaultResult instanceof Quantity<?>) {
         sb.append(((Quantity<?>) defaultResult).getNumber());
      } else {
         sb.append(defaultResult);
      }

      Object currentResult;
      for (String resultName : resultNames) {
         sb.append(delimiter);
         currentResult = results.get(resultName);
         if (currentResult instanceof Quantity<?>) {
            sb.append(((Quantity<?>) currentResult).getNumber());
         } else {
            sb.append(currentResult);
         }
      }

      try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile, true))) {
         bw.append(sb.toString());
         bw.newLine();
      } catch (IOException ioe) {
         throw new ReportingException("Could not append a report to the file: " + csvFile.getPath(), ioe);
      }
   }

   /**
    * Used to read the value of path.
    * 
    * @return The path value.
    */
   public String getPath() {
      return path;
   }

   /**
    * Used to set the value of path.
    * 
    * @param path
    *           The path value to set.
    */
   public void setPath(String path) {
      this.path = path;
      this.csvFile = new File(this.path);
   }

   /**
    * Used to read the value of delimiter.
    * 
    * @return The delimiter value.
    */
   public String getDelimiter() {
      return delimiter;
   }

   /**
    * Used to set the value of delimiter.
    * 
    * @param delimiter
    *           The delimiter value to set.
    */
   public void setDelimiter(String delimiter) {
      this.delimiter = delimiter;
   }

}
